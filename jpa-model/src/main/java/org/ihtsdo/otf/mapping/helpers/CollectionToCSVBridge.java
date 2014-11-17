package org.ihtsdo.otf.mapping.helpers;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.search.bridge.StringBridge;

public class CollectionToCSVBridge implements StringBridge
{
    public String objectToString(Object value)
    {
       if(value != null)
       {
           StringBuffer buf = new StringBuffer();

           Collection<?> col = (Collection<?>)value;
           Iterator<?> it = col.iterator();
           while(it.hasNext())
           {
               String next = it.next().toString();
               buf.append(next);
               if(it.hasNext())
                   buf.append(", ");
           }
           return buf.toString();
       }
       return null;
   }
}
