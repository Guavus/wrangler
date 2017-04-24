/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.wrangler.service.filesystem;

import co.cask.cdap.api.annotation.TransactionControl;
import co.cask.cdap.api.annotation.TransactionPolicy;
import co.cask.cdap.api.annotation.UseDataSet;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.api.dataset.Dataset;
import co.cask.cdap.api.service.http.AbstractHttpServiceHandler;
import co.cask.cdap.api.service.http.HttpServiceContext;
import co.cask.cdap.api.service.http.HttpServiceRequest;
import co.cask.cdap.api.service.http.HttpServiceResponder;
import co.cask.cdap.internal.io.SchemaTypeAdapter;
import co.cask.wrangler.RequestExtractor;
import co.cask.wrangler.api.Record;
import co.cask.wrangler.api.Sampler;
import co.cask.wrangler.dataset.workspace.DataType;
import co.cask.wrangler.dataset.workspace.WorkspaceDataset;
import co.cask.wrangler.sampling.Reservoir;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.twill.filesystem.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static co.cask.wrangler.service.directive.DirectivesService.WORKSPACE_DATASET;
import static co.cask.wrangler.service.directive.DirectivesService.error;
import static co.cask.wrangler.service.directive.DirectivesService.sendJson;
import static co.cask.wrangler.service.directive.DirectivesService.success;

/**
 * A {@link ExplorerService} is a HTTP Service handler for exploring the filesystem.
 * It provides capabilities for listing file(s) and directories. It also provides metadata.
 */
public class ExplorerService extends AbstractHttpServiceHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ExplorerService.class);
  private static final Gson gson =
    new GsonBuilder().registerTypeAdapter(Schema.class, new SchemaTypeAdapter()).create();

  private Explorer explorer;

  @UseDataSet(WORKSPACE_DATASET)
  private WorkspaceDataset table;

  /**
   * Lists the content of the path specified using the {@Location}.
   *
   * @param request HTTP Request Handler
   * @param responder HTTP Response Handler
   * @param path to the location in the filesystem
   * @throws Exception
   */
  @TransactionPolicy(value = TransactionControl.EXPLICIT)
  @Path("fs/explorer")
  @GET
  public void list(HttpServiceRequest request, HttpServiceResponder responder,
                   @QueryParam("path") String path, @QueryParam("hidden") boolean hidden) throws Exception {
    try {
      Map<String, Object> listing = explorer.browse(path, hidden);
      sendJson(responder, HttpURLConnection.HTTP_OK, gson.toJson(listing));
    } catch (ExplorerException e) {
      error(responder, e.getMessage());
    }
  }

  @Path("fs/explorer/read")
  @GET
  public void read(HttpServiceRequest request, HttpServiceResponder responder,
                   @QueryParam("path") String path, @QueryParam("lines") int lines) {
    RequestExtractor extractor = new RequestExtractor(request);
    if (extractor.isContentType("text/plain")) {
      BoundedLineInputStream stream = null;
      try {
        Location location = explorer.getLocation(path);
        String workspace = String.format("%s",location.getName());
        table.createWorkspaceMeta(workspace);

        Map<String, String> properties = new HashMap<>();
        properties.put("file", location.getName());
        properties.put("uri", location.toURI().toString());
        properties.put("path", location.toURI().getPath());
        table.writeProperties(workspace, properties);

        // Iterate through lines to extract only 'limit' random lines.
        List<Record> records = new ArrayList<>();
        BoundedLineInputStream blis = BoundedLineInputStream.iterator(location.getInputStream(), "utf-8", lines);
        Sampler<String> sampler = new Reservoir<>(lines);
        Iterator<String> it = sampler.sample(blis);
        while(it.hasNext()) {
          records.add(new Record("body", it.next()));
        }

        String data = gson.toJson(records);
        table.writeToWorkspace(workspace, WorkspaceDataset.DATA_COL, DataType.RECORDS, data.getBytes(Charsets.UTF_8));
        success(responder, String.format("Successfully loaded file '%s'", path));
      } catch (ExplorerException e) {
        error(responder, e.getMessage());
      } catch (IOException e) {
        error(responder, e.getMessage());
      } catch (Exception e) {
        error(responder, e.getMessage());
      } finally {
        if (stream != null) {
          stream.close();
        }
      }
    }
  }

  @Override
  public void initialize(HttpServiceContext context) throws Exception {
    super.initialize(context);
    final HttpServiceContext ctx = context;
    this.explorer = new Explorer(new DatasetProvider() {
      @Override
      public Dataset acquire() {
        return ctx.getDataset("indexds");
      }

      @Override
      public void release(Dataset dataset) {
        ctx.discardDataset(dataset);
      }
    });
  }
}