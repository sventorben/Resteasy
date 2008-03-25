package org.resteasy.plugins.providers;

import org.resteasy.specimpl.MultivaluedMapImpl;
import org.resteasy.util.FindAnnotation;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.Encoded;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
@ProduceMime("application/x-www-form-urlencoded")
@ConsumeMime("application/x-www-form-urlencoded")
public class FormUrlEncodedProvider implements MessageBodyReader<MultivaluedMap<String, String>>, MessageBodyWriter<MultivaluedMap<String, String>>
{
   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations)
   {
      if (!type.equals(MultivaluedMap.class)) return false;
      if (genericType == null) return true;

      if (!(genericType instanceof ParameterizedType)) return false;
      ParameterizedType params = (ParameterizedType) genericType;
      if (params.getActualTypeArguments().length != 2) return false;
      return params.getActualTypeArguments().equals(String.class) && params.getActualTypeArguments()[1].equals(String.class);
   }

   public MultivaluedMap<String, String> readFrom(Class<MultivaluedMap<String, String>> type, Type genericType, MediaType mediaType, Annotation[] annotations, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException
   {

      char[] buffer = new char[100];
      StringBuffer buf = new StringBuffer();
      BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));

      int wasRead = 0;
      do
      {
         wasRead = reader.read(buffer, 0, 100);
         if (wasRead > 0) buf.append(buffer, 0, wasRead);
      } while (wasRead > -1);

      String form = buf.toString();

      boolean encoded = FindAnnotation.findAnnotation(annotations, Encoded.class) != null;

      MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();
      String[] params = form.split("&");

      for (String param : params)
      {
         if (param.indexOf('=') >= 0)
         {
            String[] nv = param.split("=");
            try
            {
               String name = URLDecoder.decode(nv[0], "UTF-8");
               if (encoded)
               {
                  formData.add(name, nv[1]);
               }
               else
               {
                  formData.add(name, URLDecoder.decode(nv[1], "UTF-8"));
               }
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
         else
         {
            try
            {
               String name = URLDecoder.decode(param, "UTF-8");
               formData.add(name, "");
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
      }
      return formData;
   }

   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations)
   {
      if (!type.equals(MultivaluedMap.class)) return false;
      if (genericType == null) return true;

      if (!(genericType instanceof ParameterizedType)) return false;
      ParameterizedType params = (ParameterizedType) genericType;
      if (params.getActualTypeArguments().length != 2) return false;
      return params.getActualTypeArguments().equals(String.class) && params.getActualTypeArguments()[1].equals(String.class);
   }

   public long getSize(MultivaluedMap<String, String> inputStream)
   {
      return -1;
   }

   public void writeTo(MultivaluedMap<String, String> formData, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException
   {
      boolean encoded = FindAnnotation.findAnnotation(annotations, Encoded.class) != null;
      OutputStreamWriter writer = new OutputStreamWriter(entityStream, "UTF-8");

      boolean first = false;
      for (Map.Entry<String, List<String>> entry : formData.entrySet())
      {
         if (first) first = true;
         else writer.write("&");
         String encodedName = URLEncoder.encode(entry.getKey(), "UTF-8");
         for (String value : entry.getValue())
         {
            if (!encoded) value = URLEncoder.encode(value, "UTF-8");
            writer.write(encodedName);
            writer.write("=");
            writer.write(value);
         }
      }

   }

}