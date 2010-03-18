package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;

import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.dom4j.DocumentException;

public class ConfiguredRecordStorage extends JXJFreeGenericRecordStorage {
	protected ConfiguredRecordStorage(Record r,ServicesConnection conn) throws InvalidJXJException, DocumentException, IOException {
		String[] mini_key=new String[]{r.getMiniNumber().getID(),r.getMiniSummary().getID()};
		String[] mini_xml=new String[]{r.getMiniNumber().getServicesTag(),r.getMiniSummary().getServicesTag()};
		String[] mini_value=new String[]{"number","summary"};
		boolean[] xxx_mini_deurn=new boolean[]{r.getMiniNumber().isAutocomplete(),r.getMiniSummary().isAutocomplete()};
		init(conn,r,r.getServicesURL(),r.getServicesListPath(),mini_key,mini_xml,mini_value,xxx_mini_deurn);
	}
}
