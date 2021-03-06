package org.wanna.jabbot.binding.xmpp.command;

import org.jivesoftware.smack.XMPPConnection;
import org.wanna.jabbot.binding.Binding;
import org.wanna.jabbot.binding.BindingAware;
import org.wanna.jabbot.command.Command;
import org.wanna.jabbot.command.parser.ArgsParser;
import org.wanna.jabbot.command.parser.QuotedStringArgParser;

/**
 * @author Vincent Morsiani [vmorsiani@voxbone.com]
 * @since 2015-09-08
 */
public abstract class XmppCommand implements Command, BindingAware{
    protected Binding<XMPPConnection> binding;
    private final String commandName;

    protected XmppCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public ArgsParser getArgsParser() {
        return new QuotedStringArgParser();
    }

    @Override
    public void setBinding(Binding binding) {
        this.binding = binding;
    }
}
