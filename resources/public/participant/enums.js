
// Enums for arrow state


var ArrowDrawing = {
    notStarted: "select-arrow",
    typeSelected: "select-start",
    tailPositioned: "select-end"
};


var PositionOnCanvas = {
    left: "left",
    right: "right",
    center: "center"
};


// Enums for HTML IDs and names
var CanvasID = {
    practice: "mapping-canvas-practice",
    mapping1: "mapping-canvas-drivers",
    mapping2: "mapping-canvas-consequences",
    barChart: "bar-chart-canvas"
};

var ScreenID = {
    menu: "menu",
    consent: "consent",
    video: "display-video",
    mappingPractice: "mapping-practice",
    mapping1: "mapping-drivers",
    mapping2: "mapping-consequences",
    barChart: "bar-chart-drawing",
    thankYou: "thank-you"
};


var HtmlID = {
    video: "video",
    audio: "audio",
    session: "session",
    comment: "comment"
};

var HtmlClass = {
    screen: "screen"
};


// Other Enums

var BrowserStorageKey = function(studyID) {
    return {
        sessionStart: "mtool_study_" + studyID + "_session_start",
        userID: "mtool_study_" + studyID + "_mtool_user_id",
        settings: "mtool_study_" + studyID + "_mtool_settings"
    }

};

var MappingType = {
    practiceMapping: "practiceMapping",
    mapping1: "mapping1",
    mapping2: "mapping2",
    barChart: "barChart"
};

var IconType = {
    connection: "connection",
    factor: "factor",
    button: "button",
    barChart: "bar-chart"
};

var Button = {
    previous: "previous",
    next: "next",
    question: "question",
    bin: "bin"
};
