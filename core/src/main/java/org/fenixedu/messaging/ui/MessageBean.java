/*
 * @(#)MessageBean.java
 *
 * Copyright 2012 Instituto Superior Tecnico
 * Founding Authors: Luis Cruz
 *
 *      https://fenix-ashes.ist.utl.pt/
 *
 *   This file is part of the Messaging Module.
 *
 *   The Messaging Module is free software: you can
 *   redistribute it and/or modify it under the terms of the GNU Lesser General
 *   Public License as published by the Free Software Foundation, either version
 *   3 of the License, or (at your option) any later version.
 *
 *   The Messaging Module is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Leneral Public License
 *   along with the Messaging Module. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.fenixedu.messaging.ui;

import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.fenixedu.bennu.core.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.messaging.domain.Message;
import org.fenixedu.messaging.domain.Message.MessageBuilder;
import org.fenixedu.messaging.domain.Message.TemplateMessageBuilder;
import org.fenixedu.messaging.domain.MessagingSystem;
import org.fenixedu.messaging.domain.Sender;
import org.fenixedu.messaging.template.annotation.DeclareMessageTemplate;
import org.fenixedu.messaging.template.annotation.TemplateParameter;

import com.google.common.base.Strings;

@DeclareMessageTemplate(id = "org.fenixedu.messaging.message.wrapper",
        description = "message.template.message.wrapper.description", subject = "message.template.message.wrapper.subject",
        text = "message.template.message.wrapper.text", html = "message.template.message.wrapper.html", parameters = {
                @TemplateParameter(id = "subjectContent",
                        description = "message.template.message.wrapper.parameter.subjectContent"),
                @TemplateParameter(id = "textContent", description = "message.template.message.wrapper.parameter.textContent"),
                @TemplateParameter(id = "htmlContent", description = "message.template.message.wrapper.parameter.htmlContent"),
                @TemplateParameter(id = "sender", description = "message.template.message.wrapper.parameter.sender"),
                @TemplateParameter(id = "recipients", description = "message.template.message.wrapper.parameter.recipients") },
        bundle = "MessagingResources")
public class MessageBean extends MessageContentBean {

    private static final long serialVersionUID = 336571169494160668L;

    private Sender sender;
    private String replyTo;
    private Set<String> bccs, recipients;
    private Locale extraBccsLocale = I18N.getLocale();

    public Sender getSender() {
        return sender;
    }

    public void setSender(final Sender sender) {
        this.sender = sender;
    }

    public Locale getExtraBccsLocale() {
        return extraBccsLocale;
    }

    public void setExtraBccsLocale(Locale extraBccsLocale) {
        this.extraBccsLocale = extraBccsLocale;
    }

    public Set<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(Set<String> recipients) {
        this.recipients = recipients;
    }

    public Set<Group> getRecipientGroups() {
        Set<String> recipientExpressions = getRecipients();
        if (recipientExpressions != null) {
            Base64.Decoder decoder = Base64.getDecoder();
            return recipientExpressions.stream().map(e -> Group.parse(new String(decoder.decode(e)))).collect(Collectors.toSet());
        }
        return null;
    }

    public void setRecipientGroups(Set<Group> recipients) {
        if (recipients != null) {
            Base64.Encoder encoder = Base64.getEncoder();
            this.recipients =
                    recipients.stream().map(g -> encoder.encodeToString(g.getExpression().getBytes()))
                            .collect(Collectors.toSet());
        } else {
            this.recipients = null;
        }
    }

    public String getBccs() {
        return MessagingSystem.Util.toEmailListString(bccs);
    }

    public void setBccs(String bccs) {
        this.bccs = MessagingSystem.Util.toEmailSet(bccs);
    }

    public Set<String> getBccsSet() {
        return bccs;
    }

    public void setBccsSet(Set<String> bccs) {
        this.bccs = bccs;
    }

    @Override
    public Set<String> validate() {
        SortedSet<String> errors = new TreeSet<String>();
        if (getSender() == null) {
            errors.add(BundleUtil.getString(BUNDLE, "error.message.validation.sender.empty"));
        }

        errors.addAll(super.validate());

        String bccs = getBccs();
        Set<String> recipients = getRecipients();
        if ((recipients == null || recipients.isEmpty()) && Strings.isNullOrEmpty(bccs)) {
            errors.add(BundleUtil.getString(BUNDLE, "error.message.validation.recipients.empty"));
        }

        if (recipients != null && !recipients.isEmpty()) {
            try {
                Base64.Decoder decoder = Base64.getDecoder();
                recipients.forEach(e -> Group.parse(new String(decoder.decode(e))));
            } catch (DomainException e) {
                String[] args = e.getArgs();
                errors.add(BundleUtil.getString(BUNDLE, "error.message.validation.recipient.erroneous", args != null
                        && args.length > 0 ? args[0] : ""));
            }
        }

        if (!Strings.isNullOrEmpty(bccs)) {
            String[] emails = bccs.split(",");
            for (String emailString : emails) {
                final String email = emailString.trim();
                if (!isValidEmailAddress(email)) {
                    errors.add(BundleUtil.getString(BUNDLE, "error.message.validation.bcc.invalid", email));
                }
            }
        }

        if (getHtmlBody() != null && !getHtmlBody().isEmpty() && !sender.getHtmlSender()) {
            errors.add(BundleUtil.getString(BUNDLE, "error.message.validation.html.forbidden"));
        }

        setErrors(errors);
        return errors;
    }

    private static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    Message send() {
        Set<String> errors = validate();
        if (errors.isEmpty()) {
            Sender sender = getSender();
            Set<Group> recipients = getRecipientGroups();
            MessageBuilder messageBuilder = Message.from(sender).extraBccLocale(extraBccsLocale);
            TemplateMessageBuilder templateBuilder =
                    messageBuilder.template("org.fenixedu.messaging.message.wrapper").parameter("sender", sender.getFromName());
            if (recipients != null && !recipients.isEmpty()) {
                templateBuilder.parameter("recipients",
                        recipients.stream().map(r -> r.getPresentationName()).sorted().collect(Collectors.toList()));
                messageBuilder.bcc(recipients);
            }
            if (getSubject() != null && !getSubject().isEmpty()) {
                templateBuilder.parameter("subjectContent", getSubject());
            }
            if (getTextBody() != null && !getTextBody().isEmpty()) {
                templateBuilder.parameter("textContent", getTextBody());
            }
            if (getHtmlBody() != null && !getHtmlBody().isEmpty()) {
                templateBuilder.parameter("htmlContent", getHtmlBody());
            }
            messageBuilder = templateBuilder.and();
            if (bccs != null) {
                messageBuilder.bcc(bccs.toArray(new String[bccs.size()]));
            }
            if (!Strings.isNullOrEmpty(replyTo)) {
                messageBuilder.replyTo(replyTo);
            }
            return messageBuilder.send();
        }
        return null;
    }
}
