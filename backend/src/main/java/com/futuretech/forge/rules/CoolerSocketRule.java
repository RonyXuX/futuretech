package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/** The cooler's mounting kit must support the CPU's socket. */
@Component
public class CoolerSocketRule implements CompatibilityRule {

    static final String NAME = "COOLER_SOCKET";

    @Override
    public List<Issue> check(BuildParts build) {
        var cpu = build.cpu();
        var cooler = build.cooler();
        if (cpu == null || cooler == null) {
            return List.of();
        }
        String socket = cpu.spec().getSocket();
        if (cooler.spec().getSupportedSockets().contains(socket)) {
            return List.of();
        }
        return List.of(Issue.error(NAME,
                cooler.name() + " doesn't support socket " + socket + ", which " + cpu.name() + " uses.",
                cooler.productId(), cpu.productId()));
    }
}
