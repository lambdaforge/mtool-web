var axisSettings;

// Layout and behaviour of bar chart screen
setupBarChart = function(w, h, settings) {
    console.log("Setting up: barChartScreen");

    if (!canvas[uistate.activeCanvas]) { // using new fabric canvas is not possible
        setupCanvas(w, h, onBarChartCanvasClicked);
    }

    var nBars = 1 + Math.floor((settings.barChart.xEnd - settings.barChart.xStart)/settings.barChart.xStepSize);
    var maxIcons = ((settings.barChart.yEnd - settings.barChart.yStart)/settings.barChart.yStepSize) - 1;

    uistate.bars = new Array(nBars).fill(settings.barChart.yStart);


    axisSettings = getAxisSettings(canvasSettings, settings, nBars, maxIcons);

    canvas[uistate.activeCanvas].clear();

    divideCanvas(canvasSettings);
    setupAxis(canvasSettings, settings);

    drawNextButton(canvasSettings, settings)

    if (previousMappingState() !== State.none) {
       drawPreviousButton(canvasSettings, settings);
    }

    canvas[uistate.activeCanvas].renderAll();

};

setupAxis = function(canvasSettings, settings) {
    console.log("Set up axis");

    var title = new fabric.Text(settings.barChart.title, {
        left: canvasSettings.mappingCenterX,
        top: axisSettings.titleSpaceY/2,
        originY: "center",
        originX: "center",
        selectable: false

    })
    canvas[uistate.activeCanvas].add(title);
    canvas[uistate.activeCanvas].bringToFront(title);


    var xAxis = getAxisArrow(axisSettings, axisSettings.w, 0);
    canvas[uistate.activeCanvas].add(xAxis);
    canvas[uistate.activeCanvas].bringToFront(xAxis);

    var yAxis = getAxisArrow(axisSettings, axisSettings.h, -90);
    canvas[uistate.activeCanvas].add(yAxis);
    canvas[uistate.activeCanvas].bringToFront(yAxis);

    setupXMarkings(axisSettings, settings);
    setupYMarkings(axisSettings, settings);
}

setupXMarkings = function(axisSettings, settings) {
    var nBars = uistate.bars.length;

    // xTicks
    for (var i=0; i<nBars; i++) {

        var tickXCenter = axisSettings.iconXCenters[i];

        var tick = new fabric.Rect({
            top: axisSettings.zeroPos.y,
            left: tickXCenter,
            width: axisSettings.tickWidth,
            height: axisSettings.tickLength,
            fill: "black",
            originY: "top",
            originX: "center",
            selectable: false
        });
        canvas[uistate.activeCanvas].add(tick);
        canvas[uistate.activeCanvas].bringToFront(tick);

        var bar = settings.barChart.xStart+i*settings.barChart.xStepSize;

        var text = new fabric.Text(String(bar), {
            left: tickXCenter,
            top: axisSettings.zeroPos.y + axisSettings.tickLength + 2,
            originY: "top",
            originX: "center",
            fontSize: 18,
            selectable: false
        });
        text.barNumber = i;
        text.on("mousedown", onLabelClicked);
        canvas[uistate.activeCanvas].add(text);
        canvas[uistate.activeCanvas].bringToFront(text);
    }

    // xTitle
    var xTitle = new fabric.Text(settings.barChart.xTitle, {
        left: axisSettings.zeroPos.x + axisSettings.w,
        top: axisSettings.zeroPos.y + axisSettings.iconSize/2,
        originY: "bottom",
        originX: "left",
        fontSize: 18,
        selectable: false
    });
    canvas[uistate.activeCanvas].add(xTitle);
    canvas[uistate.activeCanvas].bringToFront(xTitle);
};


setupYMarkings = function(axisSettings, settings){
    var maxY = ((settings.barChart.yEnd - settings.barChart.yStart)/settings.barChart.yStepSize);

    // yTicks
    for (var i=0; i<maxY+2; i++) {

        var tickYCenter = axisSettings.iconYCenters[i] + 0.5 * axisSettings.iconSize;

        var tick = new fabric.Rect({
            top: tickYCenter,
            left: axisSettings.zeroPos.x - axisSettings.tickLength,
            width: axisSettings.tickLength,
            height: axisSettings.tickWidth,
            originY: "center",
            originX: "left",
            fill: "black",
            selectable: false
        });
        canvas[uistate.activeCanvas].add(tick);
        canvas[uistate.activeCanvas].bringToFront(tick);

        var text = new fabric.Text(String(settings.barChart.yStart + i*settings.barChart.yStepSize), {
            top: tickYCenter,
            left: axisSettings.zeroPos.x - axisSettings.iconSize/2,
            originY: "center",
            originX: "center",
            fontSize: 18,
            selectable: false
        })

        canvas[uistate.activeCanvas].add(text);
        canvas[uistate.activeCanvas].bringToFront(text);

    }

    // yTitle
    var yTitle = new fabric.Text(settings.barChart.yTitle, {
        left: axisSettings.zeroPos.x - axisSettings.iconSize/4,
        top: axisSettings.zeroPos.y - axisSettings.h,
        originY: "bottom",
        originX: "right",
        fontSize: 18,
        selectable: false
    })
    canvas[uistate.activeCanvas].add(yTitle);
    canvas[uistate.activeCanvas].bringToFront(yTitle);
}

getAxisArrow = function(axisSettings, length, rotation) {

    var start = axisSettings.zeroPos;
    var headSize = axisSettings.axisHeadSize;
    var axisOutline = rightArrow(start, length,  headSize, axisSettings.axisWidth);
    var axisAttributes = { fill: "black", originY: "center", originX: "left"};

    var arrow = new fabric.Polygon(axisOutline, axisAttributes);
    arrow.selectable = false;
    arrow.centeredRotation = false;
    arrow.rotate(rotation);

    return arrow;

}

onBarChartCanvasClicked = function(event) {

    if (!event.target) {

        var pointer = canvas[uistate.activeCanvas].getPointer(event.e);
        for (var i=0; i< axisSettings.iconXCenters.length; i++) {
            var xC = axisSettings.iconXCenters[i];
            var nextYCenter = axisSettings.iconYCenters[uistate.bars[i] - settings.barChart.yStart];

            if ((Math.abs(pointer.x-xC) < axisSettings.iconSize/2) && nextYCenter
                && (Math.abs(pointer.y - nextYCenter) < axisSettings.iconSize/2)){
                console.log("Drawing chart");
                drawBarChartIcon(settings.barChart.image, i, uistate.bars[i], axisSettings.iconSize, xC, nextYCenter + 0.5 * i);
                uistate.bars[i] += 1;
                break;
            }

        }
    } else {
        console.log("Object hit or arrow selected");
    }
};


// Draw icon on canvas
drawBarChartIcon = function(url, barNumber, posInBar, iconSize, xLeft, yTop) {

    fabric.Image.fromURL( url, function(icon) {
        icon.scale(iconSize / Math.max(icon.height, icon.width));
        icon.hasControls = false;
        icon.borderColor = "transparent";
        icon.type = IconType.barChart;
        icon.barNumber = barNumber;
        icon.posInBar = posInBar;
        icon.isIcon = true;
        icon.top = yTop;
        icon.left = xLeft;
        icon.originX = "center";
        icon.originY = "middle";
        icon.on("mousedown", onBarChartIconClicked);
        icon.selectable = false;

        canvas[uistate.activeCanvas].add(icon);
        canvas[uistate.activeCanvas].bringToFront(icon);
    });
};

onLabelClicked = function(event) {
    var label = event.target;

    if ((uistate.bars[label.barNumber] - settings.barChart.yStart ) == 1) {
        var thisIconOnBar = getIconOnPos(label.barNumber, settings.barChart.yStart);

        console.log("Delete last bar icon: " + label.barNumber);
        canvas[uistate.activeCanvas].remove(thisIconOnBar);
        uistate.bars[label.barNumber] -= 1;
        canvas[uistate.activeCanvas].renderAll();
    }
};

// For deletion
onBarChartIconClicked = function(event) {
    var icon = event.target;
    console.log("Clicked bar factor: " + icon.barNumber + ", " + icon.posInBar);

    var upmostPosition = uistate.bars[icon.barNumber] - 1;
    if (icon.posInBar === upmostPosition-1) {
        var lastIconOnBar = getIconOnPos(icon.barNumber, upmostPosition);

        console.log("Delete bar icon: " + icon.barNumber + ", " + upmostPosition);
        canvas[uistate.activeCanvas].remove(lastIconOnBar);
        uistate.bars[icon.barNumber] -= 1;
        canvas[uistate.activeCanvas].renderAll();
    } else if (icon.posInBar == settings.barChart.yStart && (uistate.bars[icon.barNumber] - settings.barChart.yStart) == 1) {
        var thisIconOnBar = getIconOnPos(icon.barNumber, icon.posInBar);

        console.log("Delete bar icon: " + icon.barNumber + ", " + icon.posInBar);
        canvas[uistate.activeCanvas].remove(thisIconOnBar);
        uistate.bars[icon.barNumber] -= 1;
        canvas[uistate.activeCanvas].renderAll();
    }
};


// Retrieve certain icon
getIconOnPos = function(barNumber, posInBar) {
    var objects = canvas[uistate.activeCanvas].getObjects();
    for (var objInd = 0; objInd < objects.length; objInd++) {
        var icon = objects[objInd];
        if (icon.isIcon && icon.barNumber === barNumber && icon.posInBar === posInBar) {
            return icon;
        }
    }
};

getBarMapping = function(){
    var barInfo = [];
    for (var i=0; i<uistate.bars.length; ++i) {
        var x = settings.barChart.xStart + i*settings.barChart.xStepSize;
        var y = settings.barChart.yStart + ( (uistate.bars[i] - settings.barChart.yStart) * settings.barChart.yStepSize);
        barInfo.push([x, y]);
    }
    return barInfo;
};
