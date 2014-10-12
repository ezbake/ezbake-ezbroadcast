/*   Copyright (C) 2013-2014 Computer Sciences Corporation
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
 * limitations under the License. */

package ezbake.ezbroadcast.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.multipart.FormDataParam;

import ezbake.base.thrift.Visibility;
import ezbake.configuration.EzConfiguration;
import ezbake.configuration.EzConfigurationLoaderException;
import ezbake.ezbroadcast.core.EzBroadcaster;

@Path("/topics")
public class TopicResource {

    private static Logger logger = LoggerFactory.getLogger(TopicResource.class);

    /**
     * <p>
     * Broadcast to the topic specified by the param. Consumes multipart
     * form data consisting of the Visibility json and payload data.
     * </p>
     * 
     * @param topic the topic to broadcast to
     * @param visibilityJson json formatted Visibility thrift struct
     * @param data the payload data to be broadcast.
     * @return A response object that indicates success or failure.
     */
    @POST
    @Path("/{topic}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response broadcast(@PathParam("topic") String topic,
            @FormDataParam("visibility") String visibilityJson,
            @FormDataParam("payload") InputStream data) {

        try {
            byte[] payload = IOUtils.toByteArray(data);
            visibilityJson = URLDecoder.decode(visibilityJson, "UTF-8");
            Gson gson = new GsonBuilder().create();
            Visibility visibility = gson.fromJson(visibilityJson, Visibility.class);

            broadcastTopic(topic, visibility, payload);
        } catch (EzConfigurationLoaderException e) {
            logger.error("An error occurred while broadcasting to topic " + topic, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException ioe) {
            logger.error("An error occurred while broadcasting to topic " + topic, ioe);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();

    }

    private void broadcastTopic(String topic, Visibility visibility,
            byte[] payload) throws EzConfigurationLoaderException, IOException {

        EzConfiguration config = new EzConfiguration();

        Properties props = config.getProperties();

        EzBroadcaster broadcaster = EzBroadcaster.create(props, "some_group"); // TODO group name?

        broadcaster.registerBroadcastTopic(topic);
        try {
            broadcaster.broadcast(topic, visibility, payload);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            broadcaster.close();
        }
    }

}
