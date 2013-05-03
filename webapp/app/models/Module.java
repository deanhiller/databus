package models;

import java.util.List;

import com.alvazan.orm.api.base.anno.NoSqlEmbedded;
import com.alvazan.orm.api.base.anno.NoSqlEntity;
import com.alvazan.orm.api.base.anno.NoSqlId;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlManyToOne;
import com.alvazan.orm.api.base.anno.NoSqlOneToOne;
import com.alvazan.orm.api.z8spi.meta.DboTableMeta;

@NoSqlEntity
public class Module {

	@NoSqlIndexed
	@NoSqlId(usegenerator=false)
	private String name;

	private String url;

	@NoSqlIndexed
	@NoSqlManyToOne
	private SecurityGroup group;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setGroup(SecurityGroup group) {
		this.group = group;
	}

	public SecurityGroup getGroup() {
		return group;
	}
	
} // KeyToTableName
