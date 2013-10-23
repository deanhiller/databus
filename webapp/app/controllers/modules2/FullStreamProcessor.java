package controllers.modules2;

import java.util.Map;

import models.message.StreamEditor;
import models.message.StreamModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.DataStreamUtil;
import controllers.modules2.framework.Direction;
import controllers.modules2.framework.ReadResult;
import controllers.modules2.framework.VisitorInfo;
import controllers.modules2.framework.procs.DatabusBadRequest;
import controllers.modules2.framework.procs.ProcessorSetup;
import controllers.modules2.framework.procs.ProcessorSetupAbstract;
import controllers.modules2.framework.procs.ProxyProcessor;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.PullProcessorAbstract;

public class FullStreamProcessor extends PullProcessorAbstract {

	private static final Logger log = LoggerFactory.getLogger(FullStreamProcessor.class);

	private String encoding;
	
	@Override
	public ProcessorSetup createPipeline(String path, VisitorInfo visitor,
			ProcessorSetup useThisChild, boolean alreadyAddedInverter) {
		StreamEditor editor = DataStreamUtil.decode(encoding);
		StreamModule fakeNode = editor.getStream();
		StreamModule root = fakeNode.getStreams().get(0);
		return super.createTree(root, visitor);
	}

	@Override
	public String init(String path, ProcessorSetup nextInChain, VisitorInfo visitor, Map<String, String> options) {
		if(log.isInfoEnabled())
			log.info("initialization of splines pull processor");
		String newPath = super.init(path, nextInChain, visitor, options);

		encoding = options.get("encoding");
		if(encoding == null)
			throw new DatabusBadRequest("The parameter encoding is required with streamV1 module");

		return newPath;
	}
	
	@Override
	public ReadResult read() {
		return getChild().read();
	}
	
}
