package com.alexecollins.docker.orchestration.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@SuppressWarnings("CanBeFinal")
public class Conf {
    private static ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    @JsonProperty(required = false)
    private String tag = null;
    @JsonProperty(required = false)
    private List<Id> links = emptyList();
    @JsonProperty(required = false)
    private Packaging packaging = new Packaging();
    @JsonProperty(required = false)
    private List<String> ports = emptyList();
    @JsonProperty(required = false)
    private List<Id> volumesFrom = emptyList();
	@JsonProperty(required = false)
	private HealthChecks healthChecks = new HealthChecks();
    @JsonProperty(required = false)
    private Map<String,String> env = emptyMap();

    @JsonProperty(required = false)
    private Map<String,String> volumes = emptyMap();


    public boolean hasTag() {
        return !StringUtils.isBlank(tag);
    }

    public String getTag() {
        return tag;
    }

    public List<Id> getLinks() {
        return links;
    }

    public List<String> getPorts() {
        return ports;
    }

	public HealthChecks getHealthChecks() {
		return healthChecks;
	}

	public List<Id> getVolumesFrom() {
		return volumesFrom;
	}

	public Packaging getPackaging() {
		return packaging;
	}

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Map<String,String> getEnv() { return this.env;  }

    public Map<String, String> getVolumes() {
        return volumes;
    }

    public static Conf readFromFile(File confFile) throws IOException {
        return confFile.length() > 0 ? MAPPER.readValue(confFile, Conf.class) : new Conf();
    }
}
