package com.futuretech.forge.rules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.futuretech.forge.BuildParts;
import com.futuretech.forge.CompatibilityRule;
import com.futuretech.forge.Issue;

/** The CPU only fits a motherboard with the same socket (e.g. AM5 or LGA1851). */
@Component
public class SocketRule implements CompatibilityRule {

    static final String NAME = "SOCKET";

    @Override
    public List<Issue> check(BuildParts build) {
        var cpu = build.cpu();
        var board = build.motherboard();
        if (cpu == null || board == null) {
            return List.of();
        }
        // sockets are stored trimmed and upper-case (ProductSpecsService), so equals is enough
        String cpuSocket = cpu.spec().getSocket();
        String boardSocket = board.spec().getSocket();
        if (cpuSocket.equals(boardSocket)) {
            return List.of();
        }
        return List.of(Issue.error(NAME,
                cpu.name() + " needs a motherboard with socket " + cpuSocket + ", but " + board.name()
                        + " has socket " + boardSocket + ".",
                cpu.productId(), board.productId()));
    }
}
