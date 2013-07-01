package models.comparitors;

import java.util.Comparator;

import models.SecureSchema;

public class SecureSchemaComparitor implements Comparator<SecureSchema> {
	    @Override
	    public int compare(SecureSchema o1, SecureSchema o2) {
	        return o1.getSchemaName().compareTo(o2.getSchemaName());
	    }
}
