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

	public static void _formfield(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Map<String,Object> field = new HashMap<String,Object>();
        Object objId = args.get("objectId");
        if(objId == null || !(objId instanceof String)) {
        	objId = args.get("arg");
        	if(objId == null || !(objId instanceof String))
        		throw new IllegalArgumentException("'objectId' param must be supplied to tag xfield as a String");
        }
        Object label = args.get("label");
        Object length = args.get("length");
        Object help = args.get("help");
        if("".equals(help))
        	help = null;
        String _arg = objId.toString();
        Object id = _arg.replace('.','_');
        Object flashObj = Flash.current().get(_arg);
        Object flashArray = new String[0]; 
        if(flashObj != null && !StringUtils.isEmpty(flashObj.toString())) {
        	Object object = flashObj;
        	String s = object.toString();
        	flashArray = s.split(",");
        }
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
        
        //NOTE: since formfield is typically INSIDE text2.html or other tags, we must get the caller object
        //instead or we have no access to the field
        if(obj == null) {
        	Map props = (Map) body.getProperty("_caller");
        	if(props != null)
        		obj = props.get(pieces[0]);
        }
        
        if(obj != null){
            if(pieces.length > 1){
            	Object val = recursivelyFindValue(pieces, obj);
                field.put("value", val);
            }else{
                field.put("value", obj);
            }
        } else if(flashObj != null)
        	field.put("value", flashObj);
        
        body.setProperty("field", field);
        body.call();
    }

	private static Object recursivelyFindValue(String[] pieces, Object firstObj) {
		Object obj = firstObj;
		for(int i = 1; i < pieces.length; i++){
		    try{
		    	String method = "get"+JavaExtensions.capFirst(pieces[i]);
		    	Method getter = getDeclaredMethod(obj.getClass(), obj, method);
		        getter.setAccessible(true);
		        Object property = getter.invoke(obj);
		        if(i == (pieces.length-1)){
		        	return property;
		        }else if(property == null) {
		        	return null;
		        } else {
		        	obj = property;
		        }
		    }catch(Exception e){
		    	if (log.isWarnEnabled())
		    		log.warn("Exception processing tag on object type="+obj.getClass().getName(), e);
		    }
		}
		
		return null;
	}
	
	public static void _xfield(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        Map<String,Object> field = new HashMap<String,Object>();
        Object objId = args.get("objectId");
        if(objId == null || !(objId instanceof String)) {
        	objId = args.get("arg");
        	if(objId == null || !(objId instanceof String))
        		throw new IllegalArgumentException("'objectId' param must be supplied to tag xfield as a String");
        }
        Object label = args.get("label");
        Object length = args.get("length");
        Object help = args.get("help");
        if("".equals(help))
        	help = null;
        String _arg = objId.toString();
        Object id = _arg.replace('.','_');
        Object flashObj = Flash.current().get(_arg);
        Object flashArray = new String[0]; 
        if(flashObj != null && !StringUtils.isEmpty(flashObj.toString())) {
        	Object object = flashObj;
        	String s = object.toString();
        	flashArray = s.split(",");
        }
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
        } else if(flashObj != null)
        	field.put("value", flashObj);
        
        body.setProperty("field", field);
        body.call();
    }

	private static Method getDeclaredMethod(Class clazz, Object obj, String method) throws NoSuchFieldException {
		if(clazz == Object.class)
			throw new NoSuchFieldException("There is no such method="+method+" on the object="+obj+" (obj is of class="+obj.getClass()+")");
		try {
			return clazz.getDeclaredMethod(method);
		} catch(NoSuchMethodException e) {
			return getDeclaredMethod(clazz.getSuperclass(), obj, method);
		}
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