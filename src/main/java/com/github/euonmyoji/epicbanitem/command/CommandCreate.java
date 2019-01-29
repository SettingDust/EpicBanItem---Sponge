package com.github.euonmyoji.epicbanitem.command;

import com.github.euonmyoji.epicbanitem.EpicBanItem;
import com.github.euonmyoji.epicbanitem.check.CheckRule;
import com.github.euonmyoji.epicbanitem.check.CheckRuleService;
import com.github.euonmyoji.epicbanitem.util.NbtTagDataUtil;
import com.github.euonmyoji.epicbanitem.util.TextUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.entity.ArmorEquipable;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.function.Predicate;

import static org.spongepowered.api.command.args.GenericArguments.*;

/**
 * @author yinyangshi GiNYAi ustc_zzzz
 */
@NonnullByDefault
class CommandCreate extends AbstractCommand {

    CommandCreate() {
        super("create", "c");
    }

    static Optional<BlockSnapshot> getBlockLookAt(CommandSource src) {
        if (src instanceof Entity) {
            Predicate<BlockRayHit<World>> filter = BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1);
            BlockRay.BlockRayBuilder<World> blockRayBuilder = BlockRay.from((Entity) src).stopFilter(filter);
            return blockRayBuilder.distanceLimit(5).build().end().map(h -> h.getLocation().createSnapshot());
        }
        return Optional.empty();
    }

    static Optional<Tuple<HandType, ItemStack>> getItemInHand(CommandSource src) {
        if (src instanceof ArmorEquipable && src instanceof Locatable) {
            for (HandType handType : Sponge.getRegistry().getAllOf(HandType.class)) {
                Optional<ItemStack> handItem = ((ArmorEquipable) src).getItemInHand(handType);
                if (handItem.isPresent() && !handItem.get().isEmpty()) {
                    return Optional.of(Tuple.of(handType, handItem.get()));
                }
            }
        }
        return Optional.empty();
    }

    static void setItemInHand(CommandSource src, Tuple<HandType, ItemStack> item) {
        if (src instanceof ArmorEquipable && src instanceof Locatable) {
            ((ArmorEquipable) src).setItemInHand(item.getFirst(), item.getSecond());
        }
    }

    @Override
    public CommandElement getArgument() {
        return seq(string(Text.of("rule-name")),
                flags().flag("-no-capture").buildWith(none()),
                flags().flag("-all-nbt").buildWith(none()),
                optional(remainingRawJoinedStrings(Text.of("query-rule"))));
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        boolean noCapture = args.hasAny("no-capture");
        boolean allNbt = args.hasAny("-all-nbt");
        //noinspection OptionalGetWithoutIsPresent
        String name = args.<String>getOne("rule-name").get();
        // TODO: use histories in Query?
        String query = args.<String>getOne("query-rule").orElse("{}");
        try {
            Optional<Tuple<HandType, ItemStack>> handItem = getItemInHand(src);
            ConfigurationNode queryNode = TextUtil.serializeStringToConfigNode(query);
            Optional<String> id = Optional.ofNullable(queryNode.getNode("id").getString());
            CheckRuleService service = Sponge.getServiceManager().provideUnchecked(CheckRuleService.class);
            if (!noCapture) {
                if (handItem.isPresent()) {
                    if (id.isPresent()) {
                        throw new CommandException(getMessage("override"));
                    } else {
                        String s = handItem.get().getSecond().getType().getId();
                        queryNode.getNode("id").setValue(s);
//                        id = Optional.of(s); //This is not used
                    }
                } else {
                    if (!id.isPresent()) {
                        throw new CommandException(getMessage("empty"));
                    }
                }
            }
            //noinspection PointlessBooleanExpression,ConstantConditions todo: 怎么copy 是这样么
            if (false && allNbt && handItem.isPresent()) {
                DataContainer nbt = NbtTagDataUtil.toNbt(handItem.get().getSecond());
                String s = TextUtil.serializeNbtToString(nbt).toPlain();
                ConfigurationNode node = TextUtil.serializeStringToConfigNode(s);
                node.removeChild("id");
                queryNode.setValue(node);
            }
            service.appendRule(new CheckRule(name, queryNode)).thenRun(() -> {
                Text succeedMessage = getMessage("succeed", "rule_name", name);
                src.sendMessage(succeedMessage);
            });
            return CommandResult.success();
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            EpicBanItem.getLogger().error("Failed to create.", e);
            throw new CommandException(getMessage("failed"), e);
        }
    }
}
