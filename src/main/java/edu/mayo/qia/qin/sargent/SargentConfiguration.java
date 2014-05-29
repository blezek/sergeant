package edu.mayo.qia.qin.sargent;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class SargentConfiguration extends Configuration {

  @Valid
  @NotNull
  @JsonProperty
  public List<Worker> services = new ArrayList<Worker>();

  @Valid
  @JsonProperty
  Integer reloadTimeInSeconds = new Integer(30);

}
