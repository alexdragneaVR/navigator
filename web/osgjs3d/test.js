'use strict';

var OSG = window.OSG;
var osg = OSG.osg;
var osgDB = OSG.osgDB;
var osgViewer = OSG.osgViewer;

var main = function() {
    // The 3D canvas.
    var canvas = document.getElementById('View');
    var viewer;

    // The viewer
    viewer = new osgViewer.Viewer(canvas);
    viewer.init();
    var rootNode = new osg.Node();
    viewer.setSceneData(rootNode);
    viewer.setupManipulator();
    viewer.run();

    var fixture = osgDB.readNodeURL('./3d/fixture1.osgjs');
    var option1 = osgDB.readNodeURL('./3d/option1.osgjs');
    var option2 = osgDB.readNodeURL('./3d/option2.osgjs');
    var option3 = osgDB.readNodeURL('./3d/option3.osgjs');
    var option4 = osgDB.readNodeURL('./3d/option4.osgjs');

    function addModel(model){
        var mt = new osg.MatrixTransform();
        osg.mat4.rotateZ(mt.getMatrix(), mt.getMatrix(), -Math.PI);

        mt.addChild(model);

        rootNode.addChild(mt);
    }

    function addModelWithOffset(model, offset){
        var mt = new osg.MatrixTransform();
        osg.mat4.rotateZ(mt.getMatrix(), mt.getMatrix(), -Math.PI + offset/10);

        mt.addChild(model);

        rootNode.addChild(mt);
    }
    Promise.all([fixture, option1, option2, option3, option4]).then(function(models) {
          addModel(models[0]);

          for(var i = 1; i < models.length ; i++){
            var option = models[i];
            for(var j = 0; j < 10 ; j++){
              addModelWithOffset(option, j);
            }
          }

          viewer.getManipulator().computeHomePosition();
    });

};

window.addEventListener('load', main, true);
