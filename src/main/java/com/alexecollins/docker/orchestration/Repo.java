package com.alexecollins.docker.orchestration;

import com.alexecollins.docker.orchestration.model.Conf;
import com.alexecollins.docker.orchestration.model.Id;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@SuppressWarnings("CanBeFinal")
class Repo {

    private static final Logger LOG = LoggerFactory.getLogger(Repo.class);

	private final DockerClient docker;
	private final String prefix;
	private final File src;
	private final Map<Id, Conf> confs = new HashMap<Id, Conf>();

    @SuppressWarnings("ConstantConditions")
	Repo(DockerClient docker, String prefix, File src){

        if (docker == null) {throw new IllegalArgumentException("docker is null");}
		if (prefix == null) {throw new IllegalArgumentException("prefix is null");}
		if (src == null) {throw new IllegalArgumentException("src is null");}
		if (!src.isDirectory()) {throw new IllegalArgumentException("src " + src + " does not exist or is directory");}

		this.docker = docker;
		this.prefix = prefix;
		this.src = src;

		if (src.isDirectory()) {
			for (File file : src.listFiles()) {
				final File confFile = new File(file, "conf.yml");
                try {
                    confs.put(new Id(file.getName()), Conf.readFromFile(confFile));
                } catch (IOException e) {
                   throw new OrchestrationException(e);
                }
            }
		}
	}

	String imageName(Id id) {
		Conf conf = conf(id);
		return (conf != null && conf.hasTag())
                        ? conf.getTag()
                        : prefix + "_" + id;
	}

	String containerName(Id id) {
		return "/" + prefix + "_" + id;
	}

	List<Container> findContainers(Id id, boolean allContainers) {
		final List<Container> strings = new ArrayList<Container>();
		for (Container container : docker.listContainersCmd().withShowAll(allContainers).exec()) {
			if (container.getImage().equals(imageName(id)) || asList(container.getNames()).contains(containerName(id))) {
				strings.add(container);
			}
		}
		return strings;
	}

	public Container findContainer(Id id) {
		final List<Container> containerIds = findContainers(id, true);
		return containerIds.isEmpty() ? null : containerIds.get(0);
	}


    public String getImageId(Id id) {
        String imageName = imageName(id);
        LOG.debug("Converting {} ({}) to image id.",id.toString(),imageName);
        List<Image> images = docker.listImagesCmd().exec();
        for (Image i : images){
            for (String tag : i.getRepoTags()){
                if (tag.startsWith(imageName)){
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Using {} ({}) for {}. It matches (enough) to {}.", new Object[]{
                                i.getId(),
                                tag,
                                id.toString(),
                                imageName});
                    }
                    return i.getId();
                }
            }
        }
        return null;
    }

	boolean imageExists(Id id) throws DockerException {
		try {
			docker.inspectImageCmd(imageName(id)).exec();
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}

	File src() {
		return src;
	}


	File src(Id id) {
		return new File(src(), id.toString());
	}

	List<Id> ids(boolean reverse) {

		final List<Id> in = new LinkedList<Id>(confs.keySet());

		final Map<Id, List<Id>> links = new HashMap<Id, List<Id>>();
		for (Id id : in) {
			links.put(id, confs.get(id).getLinks());
		}

		final List<Id> out = sort(links);

		if (reverse) {
			Collections.reverse(out);
		}

		return out;
	}

	List<Id> sort(final Map<Id, List<Id>> links) {
		final List<Id> in = new LinkedList<Id>(links.keySet());
		final List<Id> out = new LinkedList<Id>();

		while (!in.isEmpty()) {
			boolean hit = false;
			for (Iterator<Id> iterator = in.iterator(); iterator.hasNext(); ) {
				final Id id = iterator.next();
				if (out.containsAll(links.get(id))) {
					out.add(id);
					iterator.remove();
					hit = true;
				}
			}
			if (!hit) {
				throw new IllegalStateException("dependency error (e.g. circular dependency) amongst " + in);
			}
		}

		return out;
	}

	Conf conf(Id id) {
		return confs.get(id);
	}
}
