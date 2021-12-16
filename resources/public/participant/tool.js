

var userID = localStorage.getItem(BrowserStorageKey(studyID).userID);
var data = localStorage.getItem(BrowserStorageKey(studyID).settings);
var settings = data ? JSON.parse(data) : data;
var canvasSettings;
var persist = true;

// Program state
var uistate = {

    session: {
        start: {
            mapping1: null,
            mapping2: null,
            barChart: null
        },
        length: {
            mapping1: 0,
            mapping2: 0,
            barChart: 0
        },
        state: State.homeScreen
    },

    newArrow: {
        startIcon: "",
        weight: null,
        state: ArrowDrawing.notStarted
    },
    disabledIcon: "",

    iconPositions: [],
    selectedIcon: "", // used to be highlight
    audioCue: false,
    blockUI: false,
    activeCanvas: CanvasID.practice,
    bars: []

};


// Load video file and activate DOM video element
playVideo = function (videoFile) {
    var video = getVideoElement(videoFile);
    console.log("Play Video:");
    console.log(videoFile);
    video.src = videoFile;
    video.play();
};


// Load audio file and activate DOM audio element
playAudio = function (audioFile) {
    var audio = getAudioElement(audioFile);
    console.log("Play Audio:");
    console.log(audioFile);
    audio.src = audioFile;
    audio.click();
};


// Checks if an audio or video element is playing
isPlaying = function (media) {
    if (media) {
        return media.currentTime > 0 && !media.paused && !media.ended && media.readyState > 2;
    } else {
        return false;
    }
};


// Choose shown screen; stop media from previous screen
showScreen = function (screenName) {
    var screens = document.getElementsByClassName(HtmlClass.screen);
    var currentScreen = document.getElementById(screenName);
    var video = document.getElementById(HtmlID.video);
    var audio = document.getElementById(HtmlID.audio);

    // Prevent media to resume playing
    if (isPlaying(audio)) audio.pause();
    if (isPlaying(video)) video.pause();

    for (var i = 0; i < screens.length; i++) {
        screens[i].style.display = "none";
    }

    currentScreen.style.display = "block";

    console.log("Loaded screen: " + screenName);
};


// Change to video screen
displayVideo = function (videoState) {

    uistate.session.state = videoState;

    showScreen("display-video");
    switch (uistate.session.state) {
        case State.introduction: playVideo(settings.introductionVideo); break;
        case State.driversInstructions: playVideo(settings.mapping1.video); break;
        case State.consequencesInstructions: playVideo(settings.mapping2.video); break;
        case State.barChartInstructions: playVideo(settings.barChart.video); break;
        default: console.log("Unknown or non-video state: ", uistate.session.state);
    }
};


// Change to thank you screen
displayThankYouScreen = function () {
    uistate.session.state = State.thankYouScreen;

    showScreen(ScreenID.thankYou);
    playAudio(settings.thankYouAudio);
    if (persist === false) {
        console.log("Clearing local storage.")
        localStorage.clear();
    }
};


// Prepare and show mapping screen
displayMapping = function (mappingState, w, h) {
    uistate.session.state = mappingState;

    var mappingType;
    switch (mappingState) {
        case State.practiceMapping:
            showScreen(ScreenID.mappingPractice);
            uistate.activeCanvas = CanvasID.practice;
            playAudio(settings.practiceMapping.audio);
            mappingType = MappingType.practiceMapping;
            break;
        case State.driversMapping:
            showScreen(ScreenID.mapping1);
            uistate.activeCanvas = CanvasID.mapping1;
            playAudio(settings.mapping1.audio);
            mappingType = MappingType.mapping1;
            break;
        case State.consequencesMapping:
            showScreen(ScreenID.mapping2);
            uistate.activeCanvas = CanvasID.mapping2;
            playAudio(settings.mapping2.audio);
            mappingType = MappingType.mapping2;
            break;
        case State.barChartInstructions:
            playVideo(settings.barChart.video);
            break;
        case State.barChartDrawing:
            showScreen(ScreenID.barChart);
            uistate.activeCanvas = CanvasID.barChart;
            playAudio(settings.barChart.audio);
            mappingType = MappingType.barChart;
            break;
        default:
            console.log("Unknown or non-mapping state." + mappingState);
            return;
    }

    console.log("Start mapping: " + mappingType);

    uistate.session.start[mappingType] = new Date();

    if (!canvas[uistate.activeCanvas]) {

        if (mappingType === MappingType.barChart) setupBarChart(w, h, settings);
        else setupMapping(mappingType, w, h, settings);
    }

    resetUIstate();
};


// Return string with surrounding double quotes
quoted = function (string) {
    return "\"" + string + "\"";
};


getAudioElement = function (src) {
    var audio = document.getElementById(HtmlID.audio);
    if (!audio) {
        var audio = document.createElement("audio");
        audio.id = HtmlID.audio;
        audio.src = src;
        document.body.appendChild(audio);

        $("#" + HtmlID.audio).on("click", function (a) {
            a.target.play();
        });
    }

    return audio;
}

getVideoElement = function (src) {
    var video = document.getElementById(HtmlID.video);
    if (!video) {
        var videoDiv = document.getElementById("display-video");
        video = document.createElement("video");
        video.id = HtmlID.video;
        video.src = src;
        video.controls = "controls";
        videoDiv.appendChild(video);

        $("#" + HtmlID.video).on("ended", function () {
            var nextState = nextSessionState();
            switch (uistate.session.state) {
                case State.introduction: displayMapping(nextState, w, h); break;
                case State.driversInstructions: displayMapping(nextState, w, h); break;
                case State.consequencesInstructions: displayMapping(nextState, w, h); break;
                case State.barChartInstructions: displayMapping(nextState, w, h); break;
                default: console.log("Unknown or non-video session state: ", uistate.session.state);
            }
        });
    }

    return video;
}

// Behaviour on start up
setUpBehaviour = function () {

    if (userID == null) {
        console.log("Initialize session");
        userID = uuidv4();
        localStorage.setItem(BrowserStorageKey(studyID).userID, userID);
        var now = new Date()
        console.log(now)
        console.log(now.toString())
        console.log(new Date(now.toString()))
        localStorage.setItem(BrowserStorageKey(studyID).sessionStart, now.toUTCString());
        uploadData("startedAt", now)
    }

    w = window.innerWidth;
    h = window.innerHeight;

    canvasSettings = getCanvasSettings(w, h, settings);

    let currentUrl = new URL(location.toString());
    if (currentUrl.searchParams.get("persist") === "false") {
        persist = false;
    }

    // On menu screen
    $("#btn-introduction").on("click", function () {
        if (settings.useConsent) {
            showScreen(ScreenID.consent);
        } else {
            if (settings.useMapping1 || settings.useMapping2) {
                displayVideo(State.introduction);
            } else {
                displayVideo(State.barChartInstructions);
            }
        }
    });

    if (settings.useConsent) {
        // On consent screen
        $("#btn-consent").on("click", function () {
            if (settings.useMapping1 || settings.useMapping2) {
                displayVideo(State.introduction);
            } else {
                displayVideo(State.barChartInstructions);
            }
        });
        $("#btn-consent-decline").on("click", function () {
            showScreen(ScreenID.menu);
        });
    }



    // On thank you screen
    if (persist === false) {
        $("#btn-ty-back").hide();
    } else {
        $("#btn-ty-back").on("click", onPreviousButtonClicked);
    }
    $("#btn-ty-to-main").on("click", function () {
        if (canvas[CanvasID.practice] !== null) {
            canvas[CanvasID.practice].clear();
            canvas[CanvasID.practice] = null;
        }
        if (canvas[CanvasID.mapping1] !== null) {
            canvas[CanvasID.mapping1].clear();
            canvas[CanvasID.mapping1] = null;
        }
        if (canvas[CanvasID.mapping2] !== null) {
            canvas[CanvasID.mapping2].clear();
            canvas[CanvasID.mapping2] = null;
        }
        if (canvas[CanvasID.barChart] !== null) {
            canvas[CanvasID.barChart].clear();
            canvas[CanvasID.barChart] = null;
        }
        showScreen(ScreenID.menu);
    });

    if (settings.useSurvey) {
        var surveyLink = document.getElementById("survey-link");
        console.log(surveyLink);
        var newLink = surveyLink.href.replace("{USER_ID}", userID);
        console.log(newLink);
        surveyLink.href = newLink;
    }

    playAudio(settings.welcomeAudio);

    showScreen(ScreenID.menu);
};


// Behaviour on start up
window.onload = function () {

    if (settings) {
        console.log("Settings fetched from browser storage:");
        console.log(settings);
        setUpBehaviour();
    } else {
        var xhttp = new XMLHttpRequest();
        xhttp.open("GET", uploadRoute + "?study-id=" + studyID, true);
        xhttp.setRequestHeader("Accept", "application/json");
        xhttp.send()
        xhttp.onreadystatechange = function () {
            if (this.readyState == 4 && this.status == 200) {
                var response = JSON.parse(this.responseText);
                settings = response["session-settings"];
                console.log("Settings fetched from server:");
                console.log(settings);

                setUpBehaviour();
            }
        };
    }
}

// Upload data, as defined in browser
uploadData = function (key, value) {
    var data = {
        "study-id": studyID,
        "session": { "id": localStorage.getItem(BrowserStorageKey(studyID).userID) }
    };
    data["session"][key] = value;

    console.log("Data to upload: ", data);

    var xhttp = new XMLHttpRequest();
    xhttp.open("POST", settings.uploadRoute, true);
    xhttp.setRequestHeader("Content-type", "application/json");
    xhttp.send(JSON.stringify(data));
};
