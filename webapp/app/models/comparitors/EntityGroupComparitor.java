package models.comparitors;

import java.util.Comparator;

import models.EntityGroup;
import models.SecureSchema;

public class EntityGroupComparitor implements Comparator<EntityGroup> {
    @Override
    public int compare(EntityGroup o1, EntityGroup o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
