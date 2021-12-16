// ---------------------------------------------------------------------
// Setting up the mapping layout
// ---------------------------------------------------------------------
var factorSettings;
var arrowSettings;

// Layout and behaviour of mapping screen
setupMapping = function(mappingType, w, h, settings) {
    console.log("Setting up: " + mappingType);

    if (!canvas[uistate.activeCanvas]) { // using new fabric canvas is not possible
        setupCanvas(w, h, onMappingCanvasClicked);
    }
    canvas[uistate.activeCanvas].clear();

    factorSettings = getFactorSettings(canvasSettings, mappingType, settings);
    arrowSettings = getArrowSettings(canvasSettings, mappingType, settings);

    // Set up arrows and factors
    divideCanvas(canvasSettings);
    setupArrows(arrowSettings);
    setupFactors(factorSettings, settings[mappingType]);

    // Setup buttons
    drawQuestionButton(canvasSettings, settings)
    drawNextButton(canvasSettings, settings)
    drawBinButton(canvasSettings, settings)

    if (previousMappingState() !== State.none) {
        drawPreviousButton(canvasSettings, settings)
    }

    if (mappingType === MappingType.practiceMapping) {
        fabric.Image.fromURL(settings.solutionImage, function(image) {
            var maxHeightScale = canvasSettings.practiceImageHeight / image.height;
            var maxWidthScale = canvasSettings.availableWidth / image.width;
            image.scale( Math.min(maxHeightScale, maxWidthScale));
            image.top = canvasSettings.practiceImageY;
            image.originX = "center";
            image.originY = "center";
            image.left = canvasSettings.practiceImageX;
            image.selectable = false;
            image.opacity = 0.5;
            canvas[uistate.activeCanvas].add(image);
            canvas[uistate.activeCanvas].bringToFront(image);
        });
    }

    canvas[uistate.activeCanvas].renderAll();

};

// Draw Arrow selection on right side of canvas
setupArrows = function(arrowSettings) {

    // Only use negative arrows if specified in settings
    var arrows = arrowSettings.arrowsUsed;
    console.log("Arrows used:");
    console.log(arrowSettings);

    for (var arrowInd = 0; arrowInd < arrows.length; arrowInd++) {
        drawMenuArrow(arrowSettings, arrowInd, arrows[arrowInd]);
    }
};


// Draw arrow in arrow menu
drawMenuArrow = function(arrowSettings, arrowInd, arrowWeight) {

    var pos = arrowSettings.positions[arrowInd];
    var style = arrowSettings.style[arrowWeight];

    var halfLength =  arrowSettings.length / 2;
    var arrowStart = { x: pos.x - halfLength, y: pos.y };

    var buttonHeight = arrowSettings.headSize + 2 * arrowSettings.buttonMargin;
    var buttonWidth = arrowSettings.length + 2 * arrowSettings.buttonMargin;

    var headSize = arrowSettings.headSize;
    var arrowOutline;
    if (arrowWeight === 0) {
        arrowOutline = leftRightArrow(arrowStart, arrowSettings.length, headSize, style.lineWidth);
    } else {
        arrowOutline = rightArrow(arrowStart, arrowSettings.length, headSize, style.lineWidth);
    }

    var arrowAttributes =  {fill: style.color,
                            originY: "center",
                            originX: "center"};

    var arrow = new fabric.Polygon(arrowOutline, arrowAttributes);
    var arrowButton = new fabric.Rect({
        top: pos.y ,
        left: pos.x,
        width: buttonWidth,
        height: buttonHeight,
        stroke: "transparent",
        originY: "center",
        originX: "center",
        fill: "transparent"
    });

    var arrowText;
    if (arrowWeight > 0 && arrowSettings.arrowsUsed.length > 3 && arrowSettings.arrowsUsed.length != 4) {
        arrowText = "+".repeat(arrowWeight);
    } else if (arrowWeight < 0 && arrowSettings.arrowsUsed.length > 3 && arrowSettings.arrowsUsed.length != 4) {
        arrowText = "-".repeat(-1 * arrowWeight);
    } else {
        arrowText = "";
    }

    var text = new fabric.Text(String(arrowText), {
            top: pos.y - style.lineWidth/2 - 10,
            left: pos.x,
            originY: "center",
            originX: "center",
            fontSize: 25
    });

    var icon;
    icon = new fabric.Group([arrow, arrowButton, text]);
    icon.iconType = "button";
    icon.iconName = "addConnection" + arrowWeight;
    icon.connectionWeight = arrowWeight;
    icon.selectable = false;
    icon.on("mousedown", onArrowButtonClicked);
    canvas[uistate.activeCanvas].add(icon);
    canvas[uistate.activeCanvas].bringToFront(icon);
};

// Layout of factor menu on left side of screen
setupFactors = function(factorSettings, mapping) {
    var positions = factorSettings.initialFactorPositions;

    var factorInd = 0;
    for (var name in mapping.icons) {
        var url = mapping.icons[name].image;
        var x = positions[factorInd].x;
        var y = positions[factorInd].y;

        drawFactorIcon(url, name, factorSettings.iconSize, x, y, false);
        factorInd += 1;
    }

    var fixedFactor = mapping.fixedIcon;
    if (fixedFactor !== "") {
        var xFixed = factorSettings.xFixedFactor[mapping.fixedIconPosition];
        var yFixed = factorSettings.yFixedFactor;
        url = mapping.fixedIcon;
        var baseNameStart = url.lastIndexOf("/") + 1;
        var extStart = url.lastIndexOf(".");
        var nameLength = extStart - baseNameStart;
        var iconName = mapping.fixedIconName ? mapping.fixedIconName : url.substr(baseNameStart, nameLength);

        drawFactorIcon(url, iconName, factorSettings.iconSize, xFixed,  yFixed, true);
    }
};


// ---------------------------------------------------------------------
// Drawing and removing of icons on canvas
// ---------------------------------------------------------------------


// Draw icon on canvas
drawFactorIcon = function(url, iconName, iconSize, xLeft, yTop, fixed) {

    fabric.Image.fromURL( url, function(icon) {
        icon.scale(iconSize / Math.max(icon.height, icon.width));
        icon.hasControls = false;
        icon.borderColor = "transparent";
        icon.top = yTop;
        icon.left = xLeft;
        icon.iconType = IconType.factor;
        icon.originX = "center";
        icon.originY = "center";
        icon.on("mousedown", onFactorIconClicked);
        icon.selectable = false; // causes immobility on dragging

        if (fixed) {
            icon.iconFixed = true;
            icon.iconName = "fg:" + iconName;
        }
        else {
            icon.iconFixed = false;
            icon.iconName = iconName;
            icon.iconHomeX = xLeft;
            icon.iconHomeY = yTop;
        }
        canvas[uistate.activeCanvas].add(icon);
        canvas[uistate.activeCanvas].bringToFront(icon);
    });
};


// Draw arrow between symbols
drawConnection = function(arrowSettings, factorSettings, startIconName, endIconName, weight) {

    if (startIconName !== endIconName) {
        var style = arrowSettings.style[weight];
        var startPos = getIconAnchor(startIconName);
        var endPos   = getIconAnchor(endIconName);
        var dirVec = getDirv(startPos, endPos);
        var factorDistance = getDist(startPos, endPos);

        var dFromCenter = 0;
        if ( factorDistance > factorSettings.minIconDistance + arrowSettings.margin)
            dFromCenter = factorSettings.iconSize / 2 + arrowSettings.margin;
        else
            dFromCenter = factorDistance / 2;

        var arrowLength = Math.max(arrowSettings.minLength, factorDistance - 2 * dFromCenter);
        var arrowStartX = startPos.x + dFromCenter * dirVec.x;
        var arrowStartY = startPos.y + dFromCenter * dirVec.y;
        var arrowStart = {x: arrowStartX, y: arrowStartY};

        var arrowOutline;
        var headSize = arrowSettings.headSize;
        if (weight === 0) {
            arrowOutline = leftRightArrow(arrowStart, arrowLength, headSize, style.lineWidth);
        } else {
            arrowOutline = rightArrow(arrowStart, arrowLength, headSize, style.lineWidth);
        }

        var angle = 180 * Math.atan2(dirVec.y, dirVec.x) / Math.PI;

        var arrow = new fabric.Polygon(arrowOutline, {fill: style.color});
        arrow.borderColor = "transparent";
        arrow.hasControls = false;
        arrow.iconType = IconType.connection;
        arrow.connectionWeight = weight;

        console.log(arrowSettings);

        var arrowText;
        if (weight > 0 && arrowSettings.arrowsUsed.length > 3 && arrowSettings.arrowsUsed.length != 4) {
            arrowText = "+".repeat(weight);
        } else if (weight < 0  && arrowSettings.arrowsUsed.length > 3 && arrowSettings.arrowsUsed.length != 4) {
            arrowText = "-".repeat(-1 * weight);
        } else {
            arrowText = "";
        }

        var textOffset = -10;

        if (angle > 90 && angle < angle <= 270) {
            textOffset = style.lineWidth/2 + 10;
        } else if (angle < -90 && angle >= -180) {
            textOffset = style.lineWidth/2 + 10;
        } else {
            textOffset = - style.lineWidth/2 - 10;
        }

        var text = new fabric.Text(String(arrowText), {
                top: arrowStartY + textOffset,
                left: arrowStartX + arrowLength/2,
                originY: "left",
                originX: "top",
                fontSize: 25
        });

        var arrowGroup = new fabric.Group([arrow, text]);
        arrowGroup.centeredRotation = false;
        arrowGroup.iconType = IconType.connection;
        arrowGroup.iconName = startIconName + "-" + endIconName;
        arrowGroup.startIconName = startIconName;
        arrowGroup.endIconName = endIconName;
        arrowGroup.connectionWeight = weight;
        arrowGroup.selectable = false;
        arrowGroup.on("mousedown", onArrowClicked);
        arrowGroup.rotate(angle);
        canvas[uistate.activeCanvas].add(arrowGroup);
        canvas[uistate.activeCanvas].bringToFront(arrowGroup);
    }
};


// Draw blue icon background
// Only one highlight at a time possible
drawHighlight = function(icon) {
    console.log("Highlight " + icon.iconName);
    removeHighlight();
    var bbox = icon.getBoundingRect();
    var margin = factorSettings.highlightMargin;
    var highlight = new fabric.Rect({
        top: bbox.top - margin / 2,
        left: bbox.left - margin / 2,
        width: bbox.width + margin,
        height: bbox.height + margin,
        selectable: false,
        fill: settings.highlightColor
    });
    highlight.iconName = "highlight";
    canvas[uistate.activeCanvas].add(highlight);
    canvas[uistate.activeCanvas].bringToFront(highlight);
    canvas[uistate.activeCanvas].bringToFront(icon);
    uistate.selectedIcon = icon.iconName;
};


// Remove arrows connected to icon
removeConnections = function(icon) {
    var connectedIcons = getFactorConnectionIcons(icon);
    for (var arrowInd = 0; arrowInd < connectedIcons.length; arrowInd++) {
        canvas[uistate.activeCanvas].remove(connectedIcons[arrowInd]);
    }
};


// Redraw arrows connected to icon
redrawConnections = function(arrowSettings, factorSettings, factor) {
    var connectedIcons = getFactorConnectionIcons(factor);
    for (var arrowInd = 0; arrowInd < connectedIcons.length; arrowInd++) {
        var connectedIcon = connectedIcons[arrowInd];
        var factors = connectedIcon.iconName.split("-");

        canvas[uistate.activeCanvas].remove(connectedIcon);
        drawConnection(arrowSettings, factorSettings, factors[0], factors[1], connectedIcon.connectionWeight);
    }
};


// Remove bounding box from icon
removeHighlight = function() {
    var icon = getIconByName("highlight");
    canvas[uistate.activeCanvas].remove(icon);
    uistate.selectedIcon = "none";
};



// ---------------------------------------------------------------------
// Mouse interactions on canvas
// ---------------------------------------------------------------------


// Behaviour when canvas is clicked
onMappingCanvasClicked = function(event) {

    console.log("Canvas clicked");

    var selectedIcon = uistate.selectedIcon;

    if (!event.target && uistate.newArrow.state === ArrowDrawing.notStarted) {

        console.log("No object hit and no arrow selected, move factor: " + selectedIcon);

        var icon = getIconByName(selectedIcon);

        removeHighlight();

        if (icon && (icon.iconType === IconType.factor) && (icon.iconFixed === false)) {
            var pointer = canvas[uistate.activeCanvas].getPointer(event.e);
            var x = pointer.x;
            var y = pointer.y;

            if (!tooCloseToOtherFactors(factorSettings, pointer) && !withinMappingArea(factorSettings, x, y)) {

                icon.left = x;
                icon.top = y;

                // Necessary for fabric.js too detect new position:
                canvas[uistate.activeCanvas].remove(icon);
                canvas[uistate.activeCanvas].add(icon);
                canvas[uistate.activeCanvas].bringToFront(icon);

                redrawConnections(arrowSettings, factorSettings, icon);
            }
        }

        // Necessary for iOS app, otherwise mousedown detected immediately over freshly moved icon
        disableIconForShortTime(selectedIcon);


    } else {
        console.log("Object hit or arrow selected");
    }
};


// Behaviour when arrow button is clicked
onArrowButtonClicked = function(event) {
    var icon = event.target;
    console.log("Clicked arrow button: " + icon.iconName);
    if (!uistate.blockUI) {
        if ( (uistate.newArrow.weight === null)
          || (uistate.newArrow.weight !== icon.connectionWeight) ) {
            resetUIstate();
            uistate.newArrow.state = ArrowDrawing.typeSelected;
            uistate.newArrow.weight = icon.connectionWeight;
            drawHighlight(icon);
        } else {
            resetUIstate();
        }
        uistate.blockUI = true;
        setTimeout(function() {
            uistate.blockUI = false
        }, 500)
    }
};


// Behaviour on arrow click
onArrowClicked = function(event) {
    var icon = event.target;
    console.log("Clicked arrow: " + icon.iconName);
    if (uistate.highlight === icon.iconName)
        removeHighlight();
    else
        drawHighlight(icon);
};


// Behaviour on icon click
onFactorIconClicked = function(event) {
    var icon = event.target;
    console.log("Clicked factor: " + icon.iconName);
    uistate.iconPositions = [
        [icon.left, icon.top]
    ];

    // Necessary for iOS
    if (uistate.disabledIcon === icon.iconName) return;

    if (uistate.highlight === icon.iconName)    removeHighlight();
    else                                        drawHighlight(icon);

    var activeMapping;
    switch (uistate.activeCanvas) {
        case CanvasID.practice: activeMapping = settings.practiceMapping; break;
        case CanvasID.mapping1: activeMapping = settings.mapping1; break;
        case CanvasID.mapping2: activeMapping = settings.mapping2; break;
        default: console.log("Active canvas is no mapping canvas");
    }

    if (uistate.audioCue && !icon.iconFixed) { // when question mark clicked

        var audioFile = activeMapping.icons[icon.iconName].audio;
        if (audioFile) {
            console.log("Playing factor audio for: " + icon.iconName);
            playAudio(audioFile);
            setTimeout(removeHighlight, 1000);
        }

        uistate.audioCue = false;
        return;
    }

    if (icon.left > canvasSettings.leftSideWidth ) {
        console.log("Factor is on canvas");
        switch (uistate.newArrow.state) {
            case ArrowDrawing.notStarted:
                break;
            case ArrowDrawing.typeSelected:
                console.log("Select start for " + icon.iconName);
                uistate.newArrow.state = ArrowDrawing.tailPositioned;
                uistate.newArrow.startIcon = icon.iconName;

                break;
            case ArrowDrawing.tailPositioned:
                if (uistate.newArrow.startIcon === icon.iconName) {
                    console.log("Undo start selection");
                    undoArrowStartSelection();
                }
                else {
                    var start = uistate.newArrow.startIcon;
                    var end = icon.iconName;
                    var weight = uistate.newArrow.weight;
                    console.log("Create connection from " + start + " to " + end);

                    drawConnection(arrowSettings, factorSettings, start, end, weight);
                    resetUIstate();
                }
                break;
            default:
                console.log("Connection state is unknown");
                break;
        }
    }
    else {
        console.log("Factor is not on canvas");
    }

    // Necessary for iOS app, otherwise mousedown detected sometimes immediately again
    disableIconForShortTime(icon.iconName)
};



// ---------------------------------------------------------------------
// Helper functions
// ---------------------------------------------------------------------

//

disableIconForShortTime = function(selectedIcon) {
    uistate.disabledIcon = selectedIcon
        setTimeout(function(){
            uistate.disabledIcon = ""
        }, 1000);
}




// Check if a factor icon is too close to the others
tooCloseToOtherFactors = function(factorSettings, pointer) {
    var factors = getIconsOfType("factor");

    for (var factorInd = 0; factorInd < factors.length; factorInd++) {

        var factor = factors[factorInd];
        var distance = getDist(pointer, {x: factor.left, y: factor.top});
        if (distance < factorSettings.minIconDistance) return true;
    }
    return false;
};


// Get arrow objects from canvas
getConnectionArrays = function(mappingType) {
    var icons = getIconsOfType(IconType.connection);

    var arrows = [];
    for (var arrowInd = 0; arrowInd < icons.length; arrowInd++) {
        var icon = icons[arrowInd];
        var names = icon.iconName.split("-");
        var icon1 = getIconByName(icon.startIconName);
        var icon2 = getIconByName(icon.endIconName);
        var name1 = (icon1.iconFixed)? names[0].substring(3) : names[0];
        var name2 = (icon2.iconFixed)? names[1].substring(3) : names[1];
        var infoArray = [name1, name2, icon.connectionWeight];
        arrows.push(infoArray);
    }
    return arrows;
};


// Get icons connected to given icon
getFactorConnectionIcons = function(factor) {
    var connections = getIconsOfType(IconType.connection);

    var connectedIcons = [];
    for (var arrowInd = 0; arrowInd < connections.length; arrowInd++) {
        var arrow = connections[arrowInd];
        if (arrow.iconName.indexOf(factor.iconName) !== -1) {
            connectedIcons.push(arrow);
        }
    }
    return connectedIcons;
};


// Revert to of arrow start icon selection
undoArrowStartSelection = function() {
    uistate.newArrow.state = ArrowDrawing.notStarted;
    uistate.newArrow.startIcon = "";
    var arrow = getIconByName("addConnection" + uistate.newArrow.weight);
    drawHighlight(arrow);
};


// Distance to edge of mapping area
withinMappingArea = function(factorSettings, x, y) {

    var passOver = { x: 0, y: 0 };

    var offset = factorSettings.iconSize / 2;

    var bottomLimit = canvasSettings.h - offset;
    var topLimit    = offset;
    var rightLimit  = canvasSettings.w - canvasSettings.rightSideWidth - offset;
    var leftLimit   = canvasSettings.leftSideWidth;

    if      (x < leftLimit)   passOver.x = x - leftLimit;
    else if (x > rightLimit)  passOver.x = x - rightLimit;
    if      (y < topLimit)    passOver.y = y - topLimit;
    else if (y > bottomLimit) passOver.y = y - bottomLimit;

    return (passOver.x !== 0 || passOver.y !== 0);
};


// Get object from canvas
getIconByName = function(iconName) {
    var objects = canvas[uistate.activeCanvas].getObjects();
    for (var objInd = 0; objInd < objects.length; objInd++) {
        var obj = objects[objInd];
        if (obj.hasOwnProperty("iconName") && obj.iconName === iconName) {
            return obj;
        }
    }
};

// Retrieve icons from canvas. Types are "factor", "connection", "button"
getIconsOfType = function(type) {
    var icons = [];
    $.each(canvas[uistate.activeCanvas].getObjects(), function(c, obj) {
        if (obj.hasOwnProperty("iconType") && obj.iconType === type) {
            icons.push(obj);
        }
    });
    return icons;
};


// Get anchor for arrow start and end
getIconAnchor = function(iconName) {
    var icon = getIconByName(iconName);
    return { x: icon.left, y: icon.top};
};


// Get Euclidean distance of two vectors
getDist = function(v, w) {
    if (v.hasOwnProperty("x")) {
        return Math.sqrt(Math.pow(w.x - v.x, 2) + Math.pow(w.y - v.y, 2));
    } else {
        return Math.sqrt(Math.pow(w.left - v.left, 2) + Math.pow(w.top - v.top, 2));
    }
};


// Get normalized direction vector between positions
getDirv = function(v, w) {
    var d = getDist(v, w);
    if (v.hasOwnProperty("x"))  return { x: (w.x    - v.x)    / d, y: (w.y   - v.y)   / d };
    else                           return { x: (w.left - v.left) / d, y: (w.top - v.top) / d };
};


// Get orthogonal vector
getOrtho = function(v, w) {
    var dVec =  getDirv(v, w);
    return { x: dVec.y, y: -dVec.x };
};


// Compares practice solution with drawn diagram
practiceSolutionCorrect = function() {
    var diagramDrawn = listOfLists2csv(getConnectionArrays().sort());
    console.log("Drawn: ");
    console.log(diagramDrawn);
    var correctDiagram = listOfLists2csv(settings.practiceSolutionArray.sort());
    console.log("Correct: ");
    console.log(correctDiagram);

    return diagramDrawn === correctDiagram;
};


// Reset state on screen change?
resetUIstate = function() {
    removeHighlight();
    uistate.newArrow.state = ArrowDrawing.notStarted;
    uistate.newArrow.weight = null;
    uistate.newArrow.startIcon = "";
    uistate.audioCue = false;
};

