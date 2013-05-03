package tags;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Validation;
import play.mvc.Scope.Flash;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

@FastTags.Namespace("")
//@FastTags.Namespace("ours")
public class TagHelp extends FastTags {
	
	private static final Logger log = LoggerFactory.getLogger(TagHelp.class);
	
	public static void _xfield(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Map<String,Object> field = new HashMap<String,Object>();
        Object objId = args.get("objectId");
        if(objId == null || !(objId instanceof String))
        	throw new IllegalArgumentException("'objectId' param must be supplied to tag xfield as a String");
        Object label = args.get("label");
        if(label == null || !(label instanceof String))
        	throw new IllegalArgumentException("'label' param must be supplied to tag xfield as a String");
        Object length = args.get("length");
        Object help = args.get("help");
        if("".equals(help))
        	help = null;
        String _arg = objId.toString();
        Object id = _arg.replace('.','_');
        Object flashObj = Flash.current().get(_arg);
        Object flashArray = new String[0]; 
        if(flashObj != null && !StringUtils.isEmpty(flashObj.toString()))
        	flashArray = field.get("flash").toString().split(",");
        Object error = Validation.error(_arg);
        Object errorClass = error != null ? "error" : "";
        field.put("name", _arg);
        field.put("label", label);
        field.put("length", length);
        field.put("help", help);
        field.put("id", id);
        field.put("flash", flashObj);
        field.put("flashArray", flashArray);
        field.put("error", error);
        field.put("errorClass", errorClass);
        String[] pieces = _arg.split("\\.");
        Object obj = body.getProperty(pieces[0]);
        if(obj != null){
            if(pieces.length > 1){
                for(int i = 1; i < pieces.length; i++){
                    try{
                    	Field f = getDeclaredField(obj.getClass(), obj, pieces[i]);
                        f.setAccessible(true);
                        if(i == (pieces.length-1)){
                            try{
                                Method getter = obj.getClass().getMethod("get"+JavaExtensions.capFirst(f.getName()));
                                field.put("value", getter.invoke(obj, new Object[0]));
                            }catch(NoSuchMethodException e){
                                field.put("value",f.get(obj).toString());
                            }
                        }else{
                            obj = f.get(obj);
                        }
                    }catch(Exception e){
                    	if (log.isWarnEnabled())
                    		log.warn("Exception processing tag on object type="+obj.getClass().getName(), e);
                    }
                }
            }else{
                field.put("value", obj);
            }
        }
        body.setProperty("field", field);
        body.call();
    }

	private static Field getDeclaredField(Class clazz, Object obj, String fieldName) throws NoSuchFieldException {
		if(clazz == Object.class)
			throw new NoSuchFieldException("There is no such field="+fieldName+" on the object="+obj+" (obj is of class="+obj.getClass()+")");
		try {
			return clazz.getDeclaredField(fieldName);
		} catch(NoSuchFieldException e) {
			return getDeclaredField(clazz.getSuperclass(), obj, fieldName);
		}
	}
	
}