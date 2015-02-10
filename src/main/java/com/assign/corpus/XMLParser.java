/*
 * LingPipe v. 3.9
 * Copyright (C) 2003-2010 Alias-i
 *
 * This program is licensed under the Alias-i Royalty Free License
 * Version 1 WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the Alias-i
 * Royalty Free License Version 1 for more details.
 *
 * You should have received a copy of the Alias-i Royalty Free License
 * Version 1 along with this program; if not, visit
 * http://alias-i.com/lingpipe/licenses/lingpipe-license-1.txt or contact
 * Alias-i, Inc. at 181 North 11th Street, Suite 401, Brooklyn, NY 11211,
 * +1 (718) 290-9170.
 */

package com.assign.corpus;

import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * An <code>XMLParser</code> adapts a handler to be used to handle
 * text extracted from an XML source.  The parser implements {@link
 * #parse(InputSource)} using an {@link XMLReader} and an {@link
 * DefaultHandler} that is constructed by means of the abstract method
 * {@link #getXMLHandler()}.  The parsing method traps
 * thrown instances of {@link SAXException} and converts them to instances of
 * {@link IOException} before rethrowing them in order to confrom to
 * the specification of {@link Parser#parse(InputSource)}.
 *
 * @author  Bob Carpenter
 * @version 3.9.3
 * @since   LingPipe2.1
 * @param <H> the type of handler to which this parser sends events
 */
public abstract class XMLParser<H extends Handler>
    extends InputSourceParser<H> {

    /**
     * Construct an XML parser with a <code>null</code> handler.
     */
    public XMLParser() {
        super();
    }

    /**
     * Construct an XML parser with the specified handler.
     *
     * @param handler Handler to use for parsing.
     */
    public XMLParser(H handler) {
        super(handler);
    }

    /**
     * Return the default handler for SAX events.  This default
     * handler should wrap the {@link Handler} specified for this
     * class and pass events to it extracted from the XML.  Typical
     * concrete implementations of this method will extract the underlying
     * handler using {@link #getHandler()} and wrap it in a default
     * handler.
     *
     * <P>This method is called exactly once in each parse method in
     * this class.  Thus dynamic updates to the underlying handler
     * may be picked up by this adapter method.
     *
     * @return SAX handler for XML parsing.
     */
    protected abstract DefaultHandler getXMLHandler();

    /**
     * Parse the specified input source.  This method uses the
     * default handler returned by {@link #getXMLHandler()}
     * method as the handler for SAX events generated by parsing
     * the specified input source.
     *
     * <P>All SAX exceptions thrown by XML parsing are converted to
     * I/O exceptions and rethrown; this step is required by the
     * parent class's specification of this method.
     *
     * @param inSource Input source to parse.
     * @throws IOException If there is an I/O exception or a SAX
     * exception raised while parsing.
     */
    @Override
    public void parse(InputSource inSource) throws IOException {
        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setFeature(VALIDATION_FEATURE,false);
            DefaultHandler xmlHandler = getXMLHandler();
            xmlReader.setContentHandler(xmlHandler);
            xmlReader.setDTDHandler(xmlHandler);
            xmlReader.setEntityResolver(xmlHandler);
            xmlReader.parse(inSource);
        } catch (SAXException e) {
            throw new IOException("SAXException=" + e);
        }
    }

    static final String VALIDATION_FEATURE
        = "http://xml.org/sax/features/validation";


}
