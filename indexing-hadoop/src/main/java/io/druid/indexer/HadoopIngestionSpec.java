/*
 * Druid - a distributed column store.
 * Copyright 2012 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.druid.indexer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.druid.indexer.hadoop.DatasourceIngestionSpec;
import io.druid.indexer.path.UsedSegmentLister;
import io.druid.segment.indexing.DataSchema;
import io.druid.segment.indexing.IngestionSpec;
import io.druid.timeline.DataSegment;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 */
public class HadoopIngestionSpec extends IngestionSpec<HadoopIOConfig, HadoopTuningConfig>
{
  private final DataSchema dataSchema;
  private final HadoopIOConfig ioConfig;
  private final HadoopTuningConfig tuningConfig;

  @JsonCreator
  public HadoopIngestionSpec(
      @JsonProperty("dataSchema") DataSchema dataSchema,
      @JsonProperty("ioConfig") HadoopIOConfig ioConfig,
      @JsonProperty("tuningConfig") HadoopTuningConfig tuningConfig
  )
  {
    super(dataSchema, ioConfig, tuningConfig);

    this.dataSchema = dataSchema;
    this.ioConfig = ioConfig;
    this.tuningConfig = tuningConfig == null ? HadoopTuningConfig.makeDefaultTuningConfig() : tuningConfig;
  }

  @JsonProperty("dataSchema")
  @Override
  public DataSchema getDataSchema()
  {
    return dataSchema;
  }

  @JsonProperty("ioConfig")
  @Override
  public HadoopIOConfig getIOConfig()
  {
    return ioConfig;
  }

  @JsonProperty("tuningConfig")
  @Override
  public HadoopTuningConfig getTuningConfig()
  {
    return tuningConfig;
  }

  public HadoopIngestionSpec withDataSchema(DataSchema schema)
  {
    return new HadoopIngestionSpec(
        schema,
        ioConfig,
        tuningConfig
    );
  }

  public HadoopIngestionSpec withIOConfig(HadoopIOConfig config)
  {
    return new HadoopIngestionSpec(
        dataSchema,
        config,
        tuningConfig
    );
  }

  public HadoopIngestionSpec withTuningConfig(HadoopTuningConfig config)
  {
    return new HadoopIngestionSpec(
        dataSchema,
        ioConfig,
        config
    );
  }

  public static HadoopIngestionSpec updateSegmentListIfDatasourcePathSpecIsUsed(
      HadoopIngestionSpec spec,
      ObjectMapper jsonMapper,
      UsedSegmentLister segmentLister
  )
      throws IOException
  {
    String dataSource = "dataSource";
    String type = "type";
    String multi = "multi";
    String children = "children";
    String segments = "segments";
    String ingestionSpec = "ingestionSpec";

    Map<String, Object> pathSpec = spec.getIOConfig().getPathSpec();
    Map<String, Object> datasourcePathSpec = null;
    if(pathSpec.get(type).equals(dataSource)) {
      datasourcePathSpec = pathSpec;
    } else if(pathSpec.get(type).equals(multi)) {
      List<Map<String, Object>> childPathSpecs = (List<Map<String, Object>>) pathSpec.get(children);
      for(Map<String, Object> childPathSpec : childPathSpecs) {
        if (childPathSpec.get(type).equals(dataSource)) {
          datasourcePathSpec = childPathSpec;
          break;
        }
      }
    }
    if (datasourcePathSpec != null) {
      Map<String, Object> ingestionSpecMap = (Map<String, Object>) datasourcePathSpec.get(ingestionSpec);
      DatasourceIngestionSpec ingestionSpecObj = jsonMapper.convertValue(ingestionSpecMap, DatasourceIngestionSpec.class);
      List<DataSegment> segmentsList = segmentLister.getUsedSegmentsForInterval(
          ingestionSpecObj.getDataSource(),
          ingestionSpecObj.getInterval()
      );
      datasourcePathSpec.put(segments, segmentsList);
    }

    return spec;
  }

}
