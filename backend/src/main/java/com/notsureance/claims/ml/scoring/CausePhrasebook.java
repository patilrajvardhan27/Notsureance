package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.LossCause;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class CausePhrasebook {

    private static final Map<LossCause, List<String>> PHRASES = Map.of(
            LossCause.COLLISION, List.of("struck a utility pole while turning at the field entrance",
                    "rear ended by a pickup on the county road", "hit a culvert hidden in tall grass",
                    "front loader bent after impact with the grain bin", "side panel crushed against a gate post"),
            LossCause.ROLLOVER, List.of("tipped over on a steep terrace slope", "rolled into the drainage ditch",
                    "overturned when the bank gave way near the creek", "cab damaged after the machine rolled on its side"),
            LossCause.FIRE, List.of("fire started near the engine compartment", "smoke and flames from the exhaust area",
                    "chaff buildup ignited in the engine bay", "burned out in the machine shed overnight",
                    "wiring harness melted and caught fire"),
            LossCause.THEFT, List.of("machine missing from the field when we arrived", "stolen from the equipment yard overnight",
                    "lock cut and the unit was taken", "trailer and implement gone, sheriff report filed",
                    "keys missing and the tractor disappeared"),
            LossCause.HAIL, List.of("hail dented the cab roof and hood", "golf ball sized hail cracked the windshield",
                    "hailstorm shattered the lights and mirrors", "pitted panels after a severe hail storm"),
            LossCause.WIND, List.of("straight line winds flipped the pivot towers", "tornado tossed debris into the cab",
                    "derecho winds knocked the shed onto the machine", "high winds blew the door off its hinges"),
            LossCause.FLOOD, List.of("flood water covered the engine and electronics", "river overflowed and submerged the yard",
                    "standing water reached the cab floor", "flash flood washed mud into the hydraulics"),
            LossCause.LIGHTNING, List.of("lightning struck the pivot control panel", "electrical surge from lightning fried the controller",
                    "lightning hit near the dryer and burned the circuit boards"),
            LossCause.VANDALISM, List.of("windows smashed and seats slashed", "sugar poured in the fuel tank",
                    "graffiti sprayed and wiring cut", "tires punctured by trespassers"),
            LossCause.MECHANICAL_BREAKDOWN, List.of("transmission failed under load", "engine seized during harvest",
                    "hydraulic pump quit and the boom dropped", "gearbox bearing failure with metal shavings in the oil",
                    "final drive broke while pulling the planter"));

    private static final List<String> FILLERS = List.of("Operator was not injured.", "Photos are attached for review.",
            "Dealer inspected the unit and gave an estimate.", "This happened during our busiest week.",
            "Neighbor witnessed the event.", "Unit has been moved to the shop.");

    private CausePhrasebook() {
    }

    static String describe(LossCause cause, Random random, boolean brief) {
        List<String> phrases = PHRASES.get(cause);
        StringBuilder text = new StringBuilder(capitalize(phrases.get(random.nextInt(phrases.size())))).append('.');
        if (brief) {
            return text.toString();
        }
        text.append(' ').append(capitalize(phrases.get(random.nextInt(phrases.size())))).append('.');
        return text.append(' ').append(FILLERS.get(random.nextInt(FILLERS.size()))).toString();
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
