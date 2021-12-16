
// Initial positions of factors
factorPositions = function(leftSideLineHeight, leftSideWidth, nFactors, factorsPerRow) {
    var positions = [];
    var horizontalIconSpace = leftSideWidth / factorsPerRow;
    for (var i = 0; i < nFactors; i++) {
        var x = horizontalIconSpace * ((i % factorsPerRow) + 0.5);
        var row = 1 + Math.floor(i/factorsPerRow);
        var y = leftSideLineHeight * (row + 0.5);
        positions.push({x: x, y: y});
    }
    return positions;
};


// Get maximum absolute value of array
getAbsoluteMax = function(array) {
    var max = 0;
    for (var i = 0; i < array.length; i++) {
        var value = Math.abs(array[i]);
        if (value > max) max = value;
    }
    return max;
};


// Use current window size to determine canvas setup
getCanvasSettings = function(w, h, settings) {

    var rightSideWidth = w/8;
    var leftSideWidth = w/4.5;

    var buttonSize = rightSideWidth/3;
    var availableHeight = h - 2*buttonSize;
    var mappingScreenWidth = w - leftSideWidth - rightSideWidth;
    var mappingCenterX = leftSideWidth + (mappingScreenWidth/2);


    return {
        h: h,
        w: w,

        availableWidth: mappingScreenWidth,
        availableHeight: availableHeight,

        rightSideWidth: rightSideWidth,
        leftSideWidth: leftSideWidth,
        borderWidth: 1,
        mappingCenterX: mappingCenterX,

        buttonSize: buttonSize,

        centerRightSide: w - rightSideWidth/2,
        centerLeftSide: leftSideWidth/2,

        practiceImageY: h * 0.15,
        practiceImageX: mappingCenterX,
        practiceImageHeight: h * 0.3,

        color: {
            sidePanels: "#EAEAEA",
            leftDivider: "#BBBBBB",
            rightDivider: "#AAAAAA"
        }
    };
};

getFactorSettings = function(canvasSettings, mappingType, settings) {
    var factorsPerRow = 3;
    var nFactors = Object.keys(settings[mappingType].icons).length;
    var nFactorLines = Math.ceil( nFactors / factorsPerRow);
    var leftSideLineHeight = h / (2 + nFactorLines);
    var iconSize = Math.min(0.9 * leftSideLineHeight, canvasSettings.leftSideWidth/3);

    return {

        leftSideLineHeight: leftSideLineHeight,

        xFixedFactor: { right: canvasSettings.w - canvasSettings.rightSideWidth - 1.5*iconSize,
                        left:                     canvasSettings.leftSideWidth  + 1.5*iconSize,
                        center:                   canvasSettings.leftSideWidth  + 0.5*canvasSettings.availableWidth},
        yFixedFactor: (mappingType === MappingType.practiceMapping)? canvasSettings.h * 3 / 4 : canvasSettings.h / 2,

        highlightMargin: leftSideLineHeight * 0.2,

        iconSize: iconSize,
        minIconDistance: iconSize,

        factorsPerRow: factorsPerRow,
        initialFactorPositions: factorPositions(leftSideLineHeight, canvasSettings.leftSideWidth, nFactors, factorsPerRow),
    };
}


// Positions of arrows on right side of screen
getArrowPositions = function(canvasSettings, nArrows) {
    var buttonSize = canvasSettings.buttonSize;
    var positions = [];
    var x = canvasSettings.w - canvasSettings.rightSideWidth/2;
    var yOffset = buttonSize;
    var verticalIconSpace = (canvasSettings.h - 2*buttonSize) / nArrows;
    for (var i = 0; i < nArrows; i++) {
        var y = yOffset + verticalIconSpace * (i + 0.5);
        positions.push({x: x, y: y});
    }
    return positions;
};


// Get weights of arrow actually used in this mapping
getUsedArrows = function(mappingType, settings) {

    var arrows = [];
    for (var weight of settings.arrowWeights) {
        if ((weight  > 0 &&  (settings.usePositiveArrows     || (mappingType === MappingType.practiceMapping) || (!settings.usePositiveArrows && !settings.useNegativeArrows))) ||
            (weight  < 0 &&  settings.useNegativeArrows     && !(mappingType === MappingType.practiceMapping)) ||
            (weight == 0 &&  settings.useDoubleHeadedArrows && !(mappingType === MappingType.practiceMapping))) {
            arrows.push(weight);
        }
    }
    return arrows;
};


// Get color and line width from arrow weight
getArrowStyles = function(canvasSettings, settings, maxAbsWeight) {

    var maxArrowWidth = canvasSettings.h/40;
    var minArrowWidth = 2;

    var arrowWidthSteps = (maxArrowWidth - minArrowWidth ) / (maxAbsWeight-1); // -1 necessary?

    var colors = settings.arrowColor;
    var styles = {};
    for (var weight of settings.arrowWeights) {
        var lineWidthFactor = (weight == 0)? 1 : Math.abs(weight);

        var color = colors.neutral;
        if ((weight !== 0) && (settings.useNegativeArrows)) {
             color = weight < 0 ? colors.negative : colors.positive;
        }
        var width = minArrowWidth + (lineWidthFactor-1) * arrowWidthSteps;

        styles[weight] = {"color": color, "lineWidth": width}
    }

    return styles;
};

getArrowSettings = function(canvasSettings, mappingType, settings) {

    var arrowHeadSize = canvasSettings.h/20;
    var arrowsUsed = getUsedArrows(mappingType, settings);
    var maxAbsWeight = getAbsoluteMax(arrowsUsed);

    return {
        positions: getArrowPositions(canvasSettings, arrowsUsed.length),

        length: 0.7 * canvasSettings.rightSideWidth,
        headSize: arrowHeadSize,
        buttonMargin: arrowHeadSize * 0.5,
        sideSpacing:  arrowHeadSize * 0.5,
        margin: arrowHeadSize * 0.25,
        minLength: arrowHeadSize,
        arrowsUsed: arrowsUsed,
        style: getArrowStyles(canvasSettings, settings, maxAbsWeight)
    };
}

getAxisSettings = function (canvasSettings, settings, nBars, maxIcons) {

    var titleSpaceY = canvasSettings.buttonSize;
    var minInterBarFactor = 0.5;
    var maxIconHeight = (canvasSettings.h - titleSpaceY) / (maxIcons + 4);
    var maxIconWidth = canvasSettings.availableWidth / (nBars + 3);
    var iconWidth = Math.min(maxIconHeight, maxIconWidth);

    var barChartHeight = (maxIcons + 2) * iconWidth;
    var barChartWidth = Math.max((nBars+1)*iconWidth, canvasSettings.availableWidth * 0.75)
    var interBarFactor = (barChartWidth - (nBars+1)*iconWidth)/ (nBars * iconWidth);
    var barChartCenter =  { x: canvasSettings.mappingCenterX,        y: titleSpaceY + ((canvasSettings.h - titleSpaceY)/2.0)};
    var barChartZeroPos = { x: barChartCenter.x - (barChartWidth/2), y: barChartCenter.y + (barChartHeight/2)};

    var iconXCenters = [];
    for (var i=0; i<nBars; i++) {
        iconXCenters[i] = barChartZeroPos.x + (i+0.5)*(interBarFactor+1)* iconWidth;
    }

    var iconYCenters = [];
    for (var i=0; i<maxIcons+2; i++) {
        iconYCenters[i] = barChartZeroPos.y - (i + 0.5)* iconWidth;
    }

    return {
        w: barChartWidth,
        h: barChartHeight,
        center:  barChartCenter,
        zeroPos: barChartZeroPos,
        iconSize: iconWidth,
        iconXCenters: iconXCenters,
        iconYCenters: iconYCenters,
        interBarFactor: interBarFactor,
        titleSpaceY: titleSpaceY,
        axisWidth: 2,
        axisHeadSize: 12,
        tickWidth: 1,
        tickLength: 6,
    };
}
