package org.capps.security;

import java.util.Base64;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.netty.handler.codec.base64.Base64Decoder;

@Provider
public class SecurityInterceptor implements ContainerRequestFilter {

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public void filter(ContainerRequestContext context) {
        String authorization = context.getHeaderString("authorization");
        try {
            String base64Auth = authorization.replace("BASIC ","");
            String decodedAuth = new String(Base64.getDecoder().decode(base64Auth));
            String[] parts = decodedAuth.split(":");
            Secret secret =kubernetesClient.secrets().withName(parts[0]).get();
            if(secret.getData().get("password").equals(parts[1])){
                throw new WrongPasswordException("Password does not match");
            }
        } catch (Exception e) {
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build());
        }
           
 
    }

    public static class WrongPasswordException extends Exception{
        public  WrongPasswordException(String message){
            super(message);
        }
    }
    
}
