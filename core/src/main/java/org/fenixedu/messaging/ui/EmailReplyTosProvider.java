/*
 * @(#)EmailReplyTosProvider.java
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
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with the Messaging Module. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package org.fenixedu.messaging.ui;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.messaging.domain.EmailBean;
import org.fenixedu.messaging.domain.ReplyTo;
import org.fenixedu.messaging.domain.Sender;

import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;

/**
 * 
 * @author Luis Cruz
 * 
 */
public class EmailReplyTosProvider implements DataProvider {

    @Override
    public Object provide(final Object source, final Object currentValue) {
        final EmailBean emailBean = (EmailBean) source;
        final Sender sender = emailBean.getSender();
        final Set<ReplyTo> replyTos = new HashSet<ReplyTo>();
        if (sender != null) {
            replyTos.addAll(sender.getConcreteReplyTos());
        }
        return replyTos;
    }

    @Override
    public Converter getConverter() {
        return null;
    }

}
