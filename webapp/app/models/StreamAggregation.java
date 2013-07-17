package models;

import java.util.ArrayList;
import java.util.List;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlEmbedded;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;
import com.alvazan.orm.api.base.anno.NoSqlOneToOne;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

@NoSqlEntity
@NoSqlQuery(name="findAll", query="select s from TABLE as s")
public class StreamAggregation {

	@NoSqlIndexed
	@NoSqlId(usegenerator=false)
	private String name;

	@NoSqlEmbedded
	private List<String> urls = new ArrayList<String>();

//	@NoSqlIndexed
//	@NoSqlManyToOne
//	private SecurityGroup group;

	@NoSqlIndexed
	@NoSqlManyToOne
	private SecureSchema schema;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

//	public void setGroup(SecurityGroup group) {
//		this.group = group;
//	}
//
//	public SecurityGroup getGroup() {
//		return group;
//	}

	public SecureSchema getSchema() {
		return schema;
	}

	public void setSchema(SecureSchema schema) {
		schema.addAggregation(this);
		this.schema = schema;
	}
	
	public static final Cursor<KeyValue<StreamAggregation>> findAll(NoSqlEntityManager mgr) {
		Query<StreamAggregation> query = mgr.createNamedQuery(StreamAggregation.class, "findAll");
		return query.getResults();
	}
	
} // KeyToTableName
