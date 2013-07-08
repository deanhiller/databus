package gov.nrel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acegisecurity.securechannel.SecureChannelProcessor;

import play.mvc.Scope.Session;

import models.Entity;
import models.EntityGroup;
import models.EntityGroupXref;
import models.PermissionType;
import models.RoleMapping;
import models.SecureResourceGroupXref;
import models.SecureSchema;
import models.SecurityGroup;
import models.EntityUser;
import models.UserType;
import models.message.DatasetColumnModel;
import models.message.DatasetType;
import models.message.RegisterMessage;
import models.message.RegisterResponseMessage;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.spi.UniqueKeyGenerator;
import com.alvazan.play.NoSql;

import controllers.ApiPostDataPoints;
import controllers.ApiRegistration;
import controllers.MySchemaLogic;
import controllers.MySchemas;
import controllers.api.ApiPostDataPointsImpl;
import controllers.api.ApiRegistrationImpl;

public class StartupGroups {

	public static final String SCHEMA1 = "schema1";
	public static final String ENTITYGROUP1 = "groupofusers";
	public static final String ENTITYGROUP2 = "groupofusersandgroups";
	public static final String ENTITYGROUP3 = "groupofusersanddeepgroups";
	public static final String ENTITYGROUP4 = "groupwithoutdean";
	private static final String FAKE_TIME_SERIES3 = "fakeTimeSeries3";
	public static final String ROBOT_USER = "robot-bacnet";
	public static final String ROBOT_KEY = "myawesomerobot key whichisunbreakable";
	
	public static void createStuff() {
		EntityUser dean = EntityUser.findByName(NoSql.em(), StartupDetailed.DEAN.getUsername());
		EntityUser justin = EntityUser.findByName(NoSql.em(), StartupDetailed.JUSTIN.getUsername());
		EntityUser admin = EntityUser.findByName(NoSql.em(), StartupDetailed.ADMIN.getUsername());
		
		EntityGroup onlyusers = StartupUtils.createEntityGroup(NoSql.em(), dean, ENTITYGROUP1, null, "group1user", "anothergroup1user", "bill", StartupDetailed.ADMIN.getUsername());
		EntityGroup usersandgroup = StartupUtils.createEntityGroup(NoSql.em(), dean, ENTITYGROUP2, onlyusers, "group2user", "bill", StartupDetailed.ADMIN.getUsername());
		EntityGroup usersandgroupdeep = StartupUtils.createEntityGroup(NoSql.em(), dean, ENTITYGROUP3, usersandgroup, "group3user", "bob", StartupDetailed.ADMIN.getUsername());
		
		//need a group that 'dean' is not in so that we can test missing permissions with a test user with no password
		StartupUtils.createEntityGroup(NoSql.em(), justin, ENTITYGROUP4, null, "group4user", "anothergroup4user", StartupDetailed.ADMIN.getUsername());
				
		NoSql.em().flush();
		
		addGroupAndRobot(onlyusers);
		
		StartupUtils.createDatabase(NoSql.em(), dean, SCHEMA1, usersandgroupdeep, usersandgroup);

		NoSql.em().flush();

		StartupDetailed.createSeriesImplWithUnitTestData(FAKE_TIME_SERIES3, 25, 30, StartupDetailed.GROUP1, StartupDetailed.DEAN);
	}
	
	private static void addGroupAndRobot(EntityGroup group) {
		
		EntityUser user = new EntityUser();
		user.setApiKey(ROBOT_KEY);
		user.setType(UserType.ROBOT);
		user.setUsername(ROBOT_USER);
		NoSql.em().fillInWithKey(user);
		
		EntityGroupXref entityRef = new EntityGroupXref(group, user, false, true);
		NoSql.em().put(entityRef);
		NoSql.em().put(group);
		NoSql.em().put(user);
		
		NoSql.em().flush();

		SecureSchema schema = SecureSchema.findByName(NoSql.em(), "supa");
		MySchemaLogic.createXref(schema, PermissionType.ADMIN.value(), group);
	}
	

}
