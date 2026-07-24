var nodeRefStingList = ["workspace://SpacesStore/aacd7543-be1f-4d0d-8d75-43be1f7d0d72", "workspace://SpacesStore/aa11c3de-8619-45d9-91c3-de8619d5d9e2"];

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

var nodes = batch.runForNodeRefs(batchName,numberOfThreads,numberOfProcessedItems,nodeRefStingList, processorFunction, runAsSystem, beforeProcessFunction, afterProcessFunction);

for each (node in nodes){
	if (!node.properties["success"]){
		logger.error(node.properties["resultMessage"]);
	}
}
