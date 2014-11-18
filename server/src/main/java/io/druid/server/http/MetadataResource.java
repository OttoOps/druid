/*
 * Druid - a distributed column store.
 * Copyright (C) 2012, 2013  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.druid.server.http;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.druid.client.DruidDataSource;
import io.druid.metadata.MetadataSegmentManager;
import io.druid.timeline.DataSegment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 */
@Path("/druid/coordinator/v1/metadata")
public class MetadataResource
{
  private final MetadataSegmentManager metadataSegmentManager;

  @Inject
  public MetadataResource(
      MetadataSegmentManager metadataSegmentManager
  )
  {
    this.metadataSegmentManager = metadataSegmentManager;
  }

  @GET
  @Path("/datasources")
  @Produces("application/json")
  public Response getDatabaseDataSources(
      @QueryParam("full") String full,
      @QueryParam("includeDisabled") String includeDisabled
  )
  {
    Response.ResponseBuilder builder = Response.status(Response.Status.OK);
    if (includeDisabled != null) {
      return builder.entity(metadataSegmentManager.getAllDatasourceNames()).build();
    }
    if (full != null) {
      return builder.entity(metadataSegmentManager.getInventory()).build();
    }

    List<String> dataSourceNames = Lists.newArrayList(
        Iterables.transform(
            metadataSegmentManager.getInventory(),
            new Function<DruidDataSource, String>()
            {
              @Override
              public String apply(DruidDataSource dataSource)
              {
                return dataSource.getName();
              }
            }
        )
    );

    Collections.sort(dataSourceNames);

    return builder.entity(dataSourceNames).build();
  }

  @GET
  @Path("/datasources/{dataSourceName}")
  @Produces("application/json")
  public Response getDatabaseSegmentDataSource(
      @PathParam("dataSourceName") final String dataSourceName
  )
  {
    DruidDataSource dataSource = metadataSegmentManager.getInventoryValue(dataSourceName);
    if (dataSource == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    return Response.status(Response.Status.OK).entity(dataSource).build();
  }

  @GET
  @Path("/datasources/{dataSourceName}/segments")
  @Produces("application/json")
  public Response getDatabaseSegmentDataSourceSegments(
      @PathParam("dataSourceName") String dataSourceName,
      @QueryParam("full") String full
  )
  {
    DruidDataSource dataSource = metadataSegmentManager.getInventoryValue(dataSourceName);
    if (dataSource == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    Response.ResponseBuilder builder = Response.status(Response.Status.OK);
    if (full != null) {
      return builder.entity(dataSource.getSegments()).build();
    }

    return builder.entity(
        Iterables.transform(
            dataSource.getSegments(),
            new Function<DataSegment, Object>()
            {
              @Override
              public Object apply(DataSegment segment)
              {
                return segment.getIdentifier();
              }
            }
        )
    ).build();
  }

  @GET
  @Path("/datasources/{dataSourceName}/segments/{segmentId}")
  @Produces("application/json")
  public Response getDatabaseSegmentDataSourceSegment(
      @PathParam("dataSourceName") String dataSourceName,
      @PathParam("segmentId") String segmentId
  )
  {
    DruidDataSource dataSource = metadataSegmentManager.getInventoryValue(dataSourceName);
    if (dataSource == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    for (DataSegment segment : dataSource.getSegments()) {
      if (segment.getIdentifier().equalsIgnoreCase(segmentId)) {
        return Response.status(Response.Status.OK).entity(segment).build();
      }
    }
    return Response.status(Response.Status.NOT_FOUND).build();
  }
}
