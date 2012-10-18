package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

public class SolrUpdateDriver {
	private HttpSolrServer solrServer;
	private SolrSettings solrSettings;

	private int autoPushCount;

	private Collection<IndexDocumentBean> addDocuments;
	private List<String> deleteList;

	public void init() {
		solrServer = new HttpSolrServer(solrSettings.getUrl());
	}

	public void addDocument(IndexDocumentBean idb) throws IndexingException {
		synchronized (addDocuments) {
			synchronized (deleteList) {
				if (deleteList.size() > 0) {
					pushDeletes();
				}

				addDocuments.add(idb);
				if (autoPushCount >= 0 && addDocuments.size() >= autoPushCount) {
					pushAddDocuments();
				}
			}
		}
	}

	public void delete(PID pid) {
		this.delete(pid.getPid());
	}

	public void delete(String pid) {
		synchronized (addDocuments) {
			synchronized (deleteList) {
				if (addDocuments.size() > 0) {
					pushAddDocuments();
				}
				deleteList.add(pid);
				if (autoPushCount >= 0 && deleteList.size() >= autoPushCount) {
					pushDeletes();
				}
			}
		}
	}

	public void deleteByQuery(String query) {
		synchronized (addDocuments) {
			synchronized (deleteList) {
				if (addDocuments.size() > 0)
					this.pushAddDocuments();
				if (deleteList.size() > 0)
					this.pushDeletes();

				try {
					synchronized (solrServer) {
						solrServer.deleteByQuery(query);
					}
				} catch (IOException e) {
					throw new IndexingException("Failed to add document batch to solr", e);
				} catch (SolrServerException e) {
					throw new IndexingException("Failed to add document batch to solr", e);
				}
			}
		}
	}

	public void pushAddDocuments() {
		synchronized (addDocuments) {
			try {
				synchronized (solrServer) {
					solrServer.addBeans(addDocuments);
				}
				addDocuments.clear();
			} catch (IOException e) {
				throw new IndexingException("Failed to add document batch to solr", e);
			} catch (SolrServerException e) {
				throw new IndexingException("Failed to add document batch to solr", e);
			}
		}
	}

	public void pushDeletes() {
		synchronized (deleteList) {
			try {
				synchronized (solrServer) {
					solrServer.deleteById(deleteList);
				}
				deleteList.clear();
			} catch (IOException e) {
				throw new IndexingException("Failed to delete batch from solr", e);
			} catch (SolrServerException e) {
				throw new IndexingException("Failed to delete batch from solr", e);
			}
		}
	}
	
	public void push() {
		synchronized (addDocuments) {
			synchronized (deleteList) {
				if (this.addDocuments.size() > 0)
					this.pushAddDocuments();
				if (this.deleteList.size() > 0)
					this.pushDeletes();
			}
		}
	}

	public void commit() {
		try {
			synchronized (solrServer) {
				solrServer.commit();
			}
		} catch (SolrServerException e) {
			throw new IndexingException("Failed to commit changes to solr", e);
		} catch (IOException e) {
			throw new IndexingException("Failed to commit changes to solr", e);
		}
	}

}
