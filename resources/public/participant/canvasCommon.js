
var canvas = {
    "mapping-canvas-practice": null,
    "mapping-canvas-drivers": null,
    "mapping-canvas-consequences": null,
    "bar-chart-canvas": null
};


// Set up canvas according to current window size
setupCanvas = function(w, h, onCanvasClicked) {

    canvas[uistate.activeCanvas] = new fabric.Canvas(uistate.activeCanvas, {
        width: w,
        height: h
    });
    canvas[uistate.activeCanvas].on("mouse:down", onCanvasClicked);
    canvas[uistate.activeCanvas].selection = false;
    canvas[uistate.activeCanvas].hoverCursor = "default";
};

// Divide canvas into 3 areas
divideCanvas = function(canvasSettings) {
    var leftSide = new fabric.Rect({
        top: 0,
        left: 0,
        width: canvasSettings.leftSideWidth,
        height: canvasSettings.h,
        fill: canvasSettings.color.sidePanels,
        selectable: false
    });
    var leftBorder = new fabric.Rect({
        top: 0,
        left: canvasSettings.leftSideWidth,
        width: canvasSettings.borderWidth,
        height: canvasSettings.h,
        fill: canvasSettings.color.leftDivider,
        selectable: false
    });
    var rightSide = new fabric.Rect({
        top: 0,
        left: canvasSettings.w - canvasSettings.rightSideWidth,
        width: canvasSettings.rightSideWidth,
        height: canvasSettings.h,
        fill: canvasSettings.color.sidePanels,
        selectable: false
    });
    var rightBorder = new fabric.Rect({
        top: 0,
        left: canvasSettings.w - canvasSettings.rightSideWidth - canvasSettings.borderWidth,
        width: canvasSettings.borderWidth,
        height: canvasSettings.h,
        fill: canvasSettings.color.rightDivider,
        selectable: false
    });

    canvas[uistate.activeCanvas].add(rightSide);
    canvas[uistate.activeCanvas].bringToFront(rightSide);
    canvas[uistate.activeCanvas].add(rightBorder);
    canvas[uistate.activeCanvas].bringToFront(rightBorder);
    canvas[uistate.activeCanvas].add(leftSide);
    canvas[uistate.activeCanvas].bringToFront(leftSide);
    canvas[uistate.activeCanvas].add(leftBorder);
    canvas[uistate.activeCanvas].bringToFront(leftBorder);
};

// Calculate vertices for arrow path, right anchor is at arrow tip
rightArrow = function(arrowStart, arrowLength, headSize, lineWidth) {
    var halfHead = headSize / 2;
    var halfLine = lineWidth / 2;
    var rightX = arrowStart.x + arrowLength;

    var arrowTip = { x: rightX,              y: arrowStart.y            };
    var headB    = { x: rightX - halfHead,   y: arrowStart.y + halfHead };
    var br       = { x: rightX - halfHead,   y: arrowStart.y + halfLine };
    var bl       = { x: arrowStart.x,        y: arrowStart.y + halfLine };
    var tl       = { x: arrowStart.x ,       y: arrowStart.y - halfLine };
    var tr       = { x: rightX - halfHead,   y: arrowStart.y - halfLine };
    var headT    = { x: rightX - halfHead,   y: arrowStart.y - halfHead };

    return [arrowTip, headB, br, bl, tl, tr, headT]
};

// Calculate vertices for arrow path, right anchor is at right arrow tip
leftRightArrow = function(arrowStart, arrowLength, headSize, lineWidth) {
    var halfHead = headSize / 2;
    var halfLine = lineWidth / 2;
    var rightX = arrowStart.x + arrowLength;
    var leftX  = arrowStart.x;

    var rArrowTip = { x: rightX,              y: arrowStart.y            };
    var rHeadB    = { x: rightX - halfHead,   y: arrowStart.y + halfHead };
    var br        = { x: rightX - halfHead,   y: arrowStart.y + halfLine };
    var bl        = { x: leftX  + halfHead,   y: arrowStart.y + halfLine };
    var lHeadB    = { x: leftX  + halfHead,   y: arrowStart.y + halfHead };
    var lArrowTip = { x: leftX,               y: arrowStart.y            };
    var lHeadT    = { x: leftX  + halfHead,   y: arrowStart.y - halfHead };
    var tl        = { x: leftX  + halfHead,   y: arrowStart.y - halfLine };
    var tr        = { x: rightX - halfHead,   y: arrowStart.y - halfLine };
    var rHeadT    = { x: rightX - halfHead,   y: arrowStart.y - halfHead };

    return [rArrowTip, rHeadB, br, bl, lHeadB, lArrowTip, lHeadT, tl, tr, rHeadT]
};


// Buttons


drawButton = function(name, image, buttonSize, xLeft, yTop, onmousedown) {

    fabric.Image.fromURL(image, function(icon) {
        icon.scaleToHeight(buttonSize);
        icon.scaleToWidth(buttonSize);
        icon.top = yTop;
        icon.left = xLeft;
        icon.originX = "center";
        icon.originY = "center";
        icon.selectable = false;
        icon.iconType = IconType.button;
        icon.iconName = name;
        icon.on("mousedown", onmousedown);
        canvas[uistate.activeCanvas].add(icon);
        canvas[uistate.activeCanvas].bringToFront(icon);
    });
};

drawNextButton = function(canvasSettings, settings) {
    var image = settings.buttonImages.next;
    var buttonSize = canvasSettings.buttonSize;
    var x = canvasSettings.centerRightSide;
    var y = canvasSettings.h - buttonSize / 2.0;

    drawButton(Button.next, image, buttonSize, x, y, onNextButtonClicked);
};

drawPreviousButton = function(canvasSettings, settings) {
    var image = settings.buttonImages.previous;
    var buttonSize = canvasSettings.buttonSize;
    var x = canvasSettings.centerLeftSide;
    var y = buttonSize / 2.0;

    drawButton(Button.previous, image, buttonSize, x, y, onPreviousButtonClicked);
};

drawBinButton = function(canvasSettings, settings) {
    var image = settings.buttonImages.bin;
    var buttonSize = canvasSettings.buttonSize;
    var x = canvasSettings.centerRightSide;
    var y = buttonSize / 2.0;

    drawButton(Button.bin, image, buttonSize, x, y, onBinButtonClicked);
};

drawQuestionButton = function(canvasSettings, settings) {
    var image = settings.buttonImages.question;
    var buttonSize = canvasSettings.buttonSize;
    var x = canvasSettings.centerLeftSide;
    var y = canvasSettings.h - buttonSize / 2.0;

    drawButton(Button.question, image, buttonSize, x, y, onQuestionButtonClicked);
};


// Behaviour when next is clicked; change to respective video instruction for next mapping
onNextButtonClicked = function() {
    console.log("Next clicked");
    var nextState = nextSessionState();

    if (uistate.session.state === State.practiceMapping) {
        if (practiceSolutionCorrect()) {
            console.log("Practice solution is correct");
            displayVideo(nextState);

        } else {
            console.log("Practice solution is incorrect");
        }
    } else {
        uploadResult(uistate.session.state);

        if (nextState === State.thankYouScreen) {
            var sessionStart = localStorage.getItem(BrowserStorageKey(studyID).sessionStart);
            var duration = Math.round(((new Date()) - (new Date(sessionStart))) / 100) / 10;
            uploadData("duration", duration);
            displayThankYouScreen();
        } else {
            displayVideo(nextState);
        }
    }

};


// Behaviour when previous is clicked
onPreviousButtonClicked = function() {
    console.log("Previous clicked");
    var nextState = previousMappingState()
    if (nextState !== State.none)
        displayMapping(nextState);
    else console.log("No previous mapping state");
};

// Behaviour when questionmark is clicked
onQuestionButtonClicked = function() {
    console.log("Question clicked");
    resetUIstate();
    uistate.audioCue = true;
    var questionIcon = getIconByName("question");
    drawHighlight(questionIcon);
};


// Behaviour when bin is clicked
onBinButtonClicked = function() {
    console.log("Bin clicked");
    if (uistate.newArrow.state === ArrowDrawing.notStarted) {
        console.log(uistate.selectedIcon);
        var icon = getIconByName(uistate.selectedIcon);
        if (icon && !icon.iconFixed) {
            if (icon.iconType === IconType.factor) {
                console.log("Removing factor:", icon.iconName);

                removeConnections(icon);

                canvas[uistate.activeCanvas].remove(icon);
                icon.left = icon.iconHomeX;
                icon.top  = icon.iconHomeY;
                canvas[uistate.activeCanvas].add(icon);
                canvas[uistate.activeCanvas].bringToFront(icon);

            } else if (icon.iconType === IconType.connection) {
                canvas[uistate.activeCanvas].remove(icon);
            }
        }
    }
    resetUIstate();
};

// Turn every list of the collection into a csv row and every item of a list into a csv cell
listOfLists2csv = function(list) {
    var res = "";
    for (var i = 0; i < list.length; i++) {
        res = res + list[i].join(settings.separator) + "\n";
    }
    return res;
};


// Save drawing and info to local storage
uploadResult = function(mappingState) {
    console.log("Upload data: ");

    var mappingType = MappingType.none;
    switch (mappingState) {
        case State.driversMapping:      mappingType = MappingType.mapping1; break;
        case State.consequencesMapping: mappingType = MappingType.mapping2; break;
        case State.barChartDrawing:     mappingType = MappingType.barChart; break;
        default: console.log("Invalid mapping state: " + mappingState); return;
    };


    var duration = Math.round((new Date() -  uistate.session.start[mappingType]) / 100) / 10;

    var baseInfo = [["Mapping Type", quoted( mappingType ) ],
                    ["Start", quoted( uistate.session.start[mappingType] ) ],
                    ["Duration", quoted( duration )]]

    if (mappingType === MappingType.barChart) {
        var newInfoArray = baseInfo.concat([["Bars"]]).concat( getBarMapping() );
    } else {
        var newInfoArray = baseInfo.concat([["Connections"]]).concat( getConnectionArrays(mappingType) );
    }

    console.log(newInfoArray);
    var newInfo = listOfLists2csv(newInfoArray);

    console.log(newInfo);
    uploadData(mappingType+"Result", newInfo);
};
