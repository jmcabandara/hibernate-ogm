/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.impl;

import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.datastore.neo4j.EmbeddedNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.embedded.transaction.impl.EmbeddedNeo4jTransactionCoordinatorBuilder;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jBasedQueryParserService;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Provides access to the Neo4j system.
 *
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final int DEFAULT_SEQUENCE_QUERY_CACHE_MAX_SIZE = 128;
	private static Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private GraphDatabaseService neo4jDb;

	private GraphDatabaseServiceFactory graphDbFactory;

	private ServiceRegistryImplementor registry;

	private EmbeddedNeo4jSequenceGenerator sequenceGenerator;

	private Integer sequenceCacheMaxSize;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.registry = serviceRegistry;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return Neo4jBasedQueryParserService.class;
	}

	@Override
	public void configure(Map cfg) {
		graphDbFactory = new EmbeddedNeo4jGraphDatabaseServiceFactoryProvider().load( cfg, registry.getService( ClassLoaderService.class ) );
		sequenceCacheMaxSize = new ConfigurationPropertyReader( cfg )
			.property( Neo4jProperties.SEQUENCE_QUERY_CACHE_MAX_SIZE, int.class )
			.withDefault( DEFAULT_SEQUENCE_QUERY_CACHE_MAX_SIZE )
			.getValue();
	}

	@Override
	public void stop() {
		neo4jDb.shutdown();
	}

	@Override
	public void start() {
		try {
			this.neo4jDb = graphDbFactory.create();
			this.sequenceGenerator = new EmbeddedNeo4jSequenceGenerator( neo4jDb, sequenceCacheMaxSize );
			this.graphDbFactory = null;
			this.sequenceCacheMaxSize = null;
		}
		catch (Exception e) {
			throw LOG.unableToStartDatastoreProvider( e );
		}
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return EmbeddedNeo4jDialect.class;
	}

	public GraphDatabaseService getDatabase() {
		return neo4jDb;
	}

	public EmbeddedNeo4jSequenceGenerator getSequenceGenerator() {
		return this.sequenceGenerator;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return EmbeddedNeo4jSchemaDefiner.class;
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder) {
		return new EmbeddedNeo4jTransactionCoordinatorBuilder( coordinatorBuilder, this );
	}
}
