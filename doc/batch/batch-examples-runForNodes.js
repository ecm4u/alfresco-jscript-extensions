var searchQuery = "select * from cmis:folder where cmis:creationDate <  '2026-05-25'";

var s = {};
s.page = {};
s.query         = searchQuery;
s.language      = 'db-cmis';
s.page.maxItems = 2;

var batchName ='MyProcessor';
var numberOfThreads = 4;
var numberOfProcessedItems = 10;

var runAsSystem = true;
var processorFunction = function process(node){
	logger.log("output from process: "+node.parent);
	if(node.hasAspect('cm:titled')){
		logger.error(node);
	}
	logger.log(node);
	node.properties["success"] = false;
	node.properties["resultMessage"] = "nodeRef: "+node.nodeRef+": some error";
};

var beforeProcessFunction = function beforeProcess(){
	logger.error("beforeProcessorFunction-"+processingNodes.length);

};

var afterProcessFunction = function afterProcess(){
	logger.error("afterProcessorFunction-"+processingNodes.length);
};

// run for a lucene search query
var nodes = search.query(s);
batch.runForNodes(batchName,numberOfThreads,numberOfProcessedItems,nodes, processorFunction, runAsSystem, beforeProcessFunction, afterProcessFunction);

for each (node in nodes){
	if (!node.properties["success"]){
		logger.error(node.properties["resultMessage"]);
	}
}
