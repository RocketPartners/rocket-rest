/*
 * Copyright (c) 2016-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 * 
 * Copyright 2008-2016 Wells Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rocketpartners.rest;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import io.rocketpartners.J;

public class JSObject
{
   LinkedHashMap<String, Property> properties = new LinkedHashMap();

   public JSObject()
   {

   }

   public JSObject(Object... nvPairs)
   {
      for (int i = 0; i < nvPairs.length - 1; i += 2)
      {
         if (i == 0 && nvPairs[i] instanceof Map)
            throw new RuntimeException("Incorrect constructor called.  Should have called JSObject(Map)");

         put(nvPairs[i] + "", nvPairs[i + 1]);
      }
   }

   public JSObject(Map map)
   {
      for (Object key : map.keySet())
      {
         put(key + "", map.get(key));
      }
   }

   public JSObject getObject(String name)
   {
      return (JSObject) get(name);
   }

   public JSArray getArray(String name)
   {
      return (JSArray) get(name);
   }

   public String getString(String name)
   {
      Object value = get(name);
      if (value != null)
         return value.toString();
      return null;
   }

   public Object get(String name)
   {
      Property p = getProperty(name);
      if (p != null)
         return p.getValue();

      return null;
   }

   public Object find(String path)
   {
      List<String> props = J.explode("\\.", path);

      Object obj = this;
      for (String prop : props)
      {
         if (obj == null)
            break;
         obj = ((JSObject) obj).get(prop);
      }
      return obj;
   }

   public Object put(String name, Object value)
   {
      Property prop = putProperty(new Property(name, value));
      return prop;
   }

   public void putAll(Map<String, Object> nvpairs)
   {
      for (String name : nvpairs.keySet())
         put(name, nvpairs.get(name));
   }

   public boolean hasProperty(String name)
   {
      return properties.containsKey(name);
   }

   public boolean containsKey(String name)
   {
      return properties.containsKey(name);
   }

   Property putProperty(Property prop)
   {
      String name = prop.getName();
      Object value = prop.getValue();

      //hack to support case insensitivity on property lookup
      for (String key : (List<String>) new ArrayList(properties.keySet()))
      {
         if (key.equalsIgnoreCase(name))
            properties.remove(key);
      }

      properties.put(name, prop);
      return prop;
   }

   public Object remove(String name)
   {
      Property old = removeProperty(name);
      return old != null ? old.getValue() : old;
   }

   public Set<String> keys()
   {
      return new LinkedHashSet(properties.keySet());
   }

   public Set<String> keySet()
   {
      return new LinkedHashSet(properties.keySet());
   }

   public Property getProperty(String name)
   {
      Property p = properties.get(name);
      if (p == null && properties.size() > 0)
      {
         //hack to support case insensitivity on property lookup
         for (String key : properties.keySet())
         {
            if (key.equalsIgnoreCase(name))
            {
               p = properties.get(key);
               break;
            }
         }
      }
      return p;
   }

   public List<Property> getProperties()
   {
      return new ArrayList(properties.values());
   }

   public Property removeProperty(String name)
   {
      Property p = properties.get(name);
      if (p == null && properties.size() > 0)
      {
         //hack to support case insensitivity on property lookup
         for (String key : properties.keySet())
         {
            if (key.equalsIgnoreCase(name))
            {
               p = properties.get(key);
               break;
            }
         }
      }
      if (p != null)
         properties.remove(p.getName());
      return p;
   }

   public static class Property
   {
      String name  = null;
      Object value = null;

      public Property(String name, Object value)
      {
         super();
         this.name = name;
         this.value = value;
      }

      public String toString()
      {
         return name + " = " + value;
      }

      /**
       * @return the name
       */
      public String getName()
      {
         return name;
      }

      /**
       * @param name the name to set
       */
      public void setName(String name)
      {
         this.name = name;
      }

      /**
       * @return the value
       */
      public Object getValue()
      {
         return value;
      }

      /**
       * @param value the value to set
       */
      public void setValue(Object value)
      {
         this.value = value;
      }
   }

   public Map asMap()
   {
      Map map = new HashMap();
      for (Property p : properties.values())
      {
         String name = p.name;
         Object value = p.value;

         if (value instanceof JSArray)
         {
            map.put(name, ((JSArray) p.getValue()).asList());
         }
         else
         {

            map.put(name, value);
         }
      }
      return map;
   }

   @Override
   public String toString()
   {
      return toString(true);
   }

   public String toString(boolean pretty)
   {
      return toString(pretty, false);
   }

   public String toString(boolean pretty, boolean lowercaseNames)
   {
      try
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         JsonGenerator json = new JsonFactory().createGenerator(baos);
         if (pretty)
            json.useDefaultPrettyPrinter();
         write(json, new HashSet(), lowercaseNames);
         json.flush();
         baos.flush();

         return new String(baos.toByteArray());
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   void write(JsonGenerator json, HashSet visited, boolean lowercaseNames) throws Exception
   {
      Property href = getProperty("href");

      if (visited.contains(this))
      {
         json.writeStartObject();
         if (href != null)
         {
            json.writeStringField("@link", href.getValue() + "");
         }

         json.writeEndObject();
         return;
      }
      visited.add(this);

      json.writeStartObject();

      if (href != null)
         json.writeStringField("href", href.getValue() + "");

      for (String key : properties.keySet())
      {
         Property p = properties.get(key);
         if (p == href)
            continue;

         if (p.value == null)
         {
            json.writeNullField(p.name);
         }
         else if (p.value instanceof JSObject)
         {
            if (!lowercaseNames)
               json.writeFieldName(p.name);
            else
               json.writeFieldName(p.name.toLowerCase());
            ((JSObject) p.value).write(json, visited, lowercaseNames);
         }
         else if (p.value instanceof Date)
         {
            json.writeStringField(p.name, J.formatDate((Date) p.value, "yyyy-MM-dd'T'HH:mmZ"));
         }
         else if (p.value instanceof BigDecimal)
         {
            json.writeNumberField(p.name, (BigDecimal) p.value);
         }
         else if (p.value instanceof Double)
         {
            json.writeNumberField(p.name, (Double) p.value);
         }
         else if (p.value instanceof Float)
         {
            json.writeNumberField(p.name, (Float) p.value);
         }
         else if (p.value instanceof Integer)
         {
            json.writeNumberField(p.name, (Integer) p.value);
         }
         else if (p.value instanceof Long)
         {
            json.writeNumberField(p.name, (Long) p.value);
         }
         else if (p.value instanceof BigDecimal)
         {
            json.writeNumberField(p.name, (BigDecimal) p.value);
         }
         else if (p.value instanceof BigInteger)
         {
            json.writeNumberField(p.name, ((BigInteger) p.value).intValue());
         }
         else if (p.value instanceof Boolean)
         {
            json.writeBooleanField(p.name, (Boolean) p.value);
         }
         else
         {
            String strVal = p.value + "";
            if ("null".equals(strVal))
            {
               json.writeNullField(p.name);
            }
            else
            {
               strVal = JS.encodeString(strVal);
               json.writeStringField(p.name, strVal);
            }
         }
      }
      json.writeEndObject();
   }

}
