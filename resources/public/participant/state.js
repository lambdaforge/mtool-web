
var State = {
    none: "",
    homeScreen: "homeScreen",
    consent: "consent",
    introduction: "introduction",
    practiceMapping: "practiceMapping",
    driversInstructions: "mapping1Instructions",
    driversMapping: "mapping1",
    consequencesInstructions: "mapping2Instructions",
    consequencesMapping: "mapping2",
    barChartInstructions: "barChartInstructions",
    barChartDrawing: "barChartDrawing",
    thankYouScreen: "thankYouScreen"
};


stateSuccession = function() {
    var states = [State.none, State.homeScreen, State.consent];

    if (settings.useMapping1 || settings.useMapping2) {
        states.push(State.introduction);
        states.push(State.practiceMapping);
    }
    if (settings.useMapping1) {
        states.push(State.driversInstructions);
        states.push(State.driversMapping);
    }
    if (settings.useMapping2) {
        states.push(State.consequencesInstructions);
        states.push(State.consequencesMapping);
    }
    if (settings.useBarChart) {
        states.push(State.barChartInstructions);
        states.push(State.barChartDrawing);
    }
    states.push(State.thankYouScreen);
    states.push(State.none);

    return states;
}

mappingStateSuccession = function() {
    var states = [State.none];

    if (settings.useMapping1) states.push(State.driversMapping);
    if (settings.useMapping2) states.push(State.consequencesMapping);
    if (settings.useBarChart) states.push(State.barChartDrawing);
    states.push(State.thankYouScreen);
    states.push(State.none);

    return states;
}

// Determine the next state
nextSessionState = function() {
    var allStates = stateSuccession();

    return allStates[allStates.indexOf(uistate.session.state) + 1];
};




// Determine the last mapping state
previousMappingState = function() {
    var mappingStates = mappingStateSuccession();
    var mappingStateIndex = mappingStates.indexOf(uistate.session.state);

    if (mappingStateIndex === -1) return State.none;
    else return mappingStates[mappingStateIndex - 1];
};
