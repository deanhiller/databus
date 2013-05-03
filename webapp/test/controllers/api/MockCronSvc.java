package controllers.api;

import java.util.List;

import org.playorm.cron.api.CronListener;
import org.playorm.cron.api.CronService;
import org.playorm.cron.api.PlayOrmCronJob;

public class MockCronSvc implements CronService {

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addListener(CronListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveMonitor(PlayOrmCronJob monitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PlayOrmCronJob getMonitor(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlayOrmCronJob> getMonitors(List<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteMonitor(String id) {
		// TODO Auto-generated method stub
		
	}

}
