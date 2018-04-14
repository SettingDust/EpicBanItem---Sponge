package com.github.euonmyoji.epicbanitem.command;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;

public class Query {

    static CommandSpec query = CommandSpec.builder()
            .permission("epicbanitem.query")
            .executor((src, args) -> {
                //something
                return CommandResult.success();
            })
            .build();
}
