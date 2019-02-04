package org.jazzcommunity.GitConnectorService.dcc.service;

import com.google.common.base.Joiner;
import com.ibm.team.repository.service.TeamRawService;
import com.siemens.bt.jazz.services.base.rest.parameters.PathParameters;
import com.siemens.bt.jazz.services.base.rest.parameters.RestRequest;
import com.siemens.bt.jazz.services.base.rest.service.AbstractRestService;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.commons.logging.Log;
import org.apache.http.entity.ContentType;
import org.jazzcommunity.GitConnectorService.dcc.data.LinkCollector;
import org.jazzcommunity.GitConnectorService.dcc.data.WorkItemLinkFactory;
import org.jazzcommunity.GitConnectorService.dcc.xml.Commits;

public class CommitService extends AbstractRestService {

  public CommitService(
      Log log,
      HttpServletRequest request,
      HttpServletResponse response,
      RestRequest restRequest,
      TeamRawService parentService,
      PathParameters pathParameters) {
    super(log, request, response, restRequest, parentService, pathParameters);
  }

  @Override
  public void execute() throws Exception {
    // print some request information for debugging
    System.out.println(request.getRemoteUser());
    System.out.println(request.getRequestURI());

    for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
      String name = names.nextElement();
      String out = String.format("Header %s: %s", name, request.getHeader(name));
      this.log.error(out);
    }

    // Example output generated by current git commit query
    // Parameter fields:
    // commits/commit/(url|commiterName|comment|sha|date|commiterEmail|repositoryKey)
    // Parameter modifiedsince: 2018-11-23T09:06:54.793-0600
    // Parameter async: true
    for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      String value = Joiner.on(", ").join(entry.getValue());
      String out = String.format("Parameter %s: %s", entry.getKey(), value);
      this.log.error(out);
    }

    Collection<WorkItemLinkFactory> links = new LinkCollector(this.parentService).collect();
    Commits commits = new Commits();

    for (WorkItemLinkFactory link : links) {
      System.out.println(String.format("Item id: %s, uuid: %s", link.getId(), link.getItemId()));
      commits.addCommits(link.resolveCommits());
    }
    response.setContentType(ContentType.APPLICATION_XML.toString());
    // dcc doesn't send a required encoding, but will error out on anything that isn't utf-8. I'm
    // not sure this is correct for every deployment configuration, but has been the same with every
    // instance that I have tested so far.
    response.setCharacterEncoding("UTF-8");

    Marshaller context = JAXBContext.newInstance(Commits.class).createMarshaller();
    // makes the output human readable. This should probably be a flag or something so that we
    // don't create additional overhead when running in production.
    context.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    context.marshal(commits, response.getWriter());
  }
}
