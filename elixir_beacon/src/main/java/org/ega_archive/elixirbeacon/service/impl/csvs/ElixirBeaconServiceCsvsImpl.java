package org.ega_archive.elixirbeacon.service.impl.csvs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.babelomics.csvs.lib.ws.QueryResponse;
import org.ega_archive.elixirbeacon.constant.CsvsConstants;
import org.ega_archive.elixirbeacon.dto.Beacon;
import org.ega_archive.elixirbeacon.dto.BeaconAlleleRequest;
import org.ega_archive.elixirbeacon.dto.BeaconAlleleResponse;
import org.ega_archive.elixirbeacon.dto.BeaconGenomicSnpRequest;
import org.ega_archive.elixirbeacon.dto.Dataset;
import org.ega_archive.elixirbeacon.enums.FilterDatasetResponse;
import org.ega_archive.elixirbeacon.enums.VariantType;
import org.ega_archive.elixirbeacon.service.ElixirBeaconService;
import org.ega_archive.elixirbeacon.service.GenomicQuery;
import org.ega_archive.elixirbeacon.utils.ParseResponse;
import org.ega_archive.elixircore.exception.NotImplementedException;
import org.ega_archive.elixircore.helper.CommonQuery;
import org.ega_archive.elixircore.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
//@Primary
@Service
public class ElixirBeaconServiceCsvsImpl implements ElixirBeaconService {

  @Autowired
  private GenomicQuery genomicQuery;

  @Autowired
  private ParseResponse parseResponse;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public Beacon listDatasets(CommonQuery commonQuery, String referenceGenome) {
    Beacon beacon = new Beacon();
    List<Dataset> datasets = genomicQuery.listDatasets();
    beacon.setDatasets(datasets);

    String url = CsvsConstants.CSVS_URL + "/files/samples";
    QueryResponse<Integer> integerQueryResponse = parseResponse
        .parseCsvsResponse(url, Integer.class);
    Integer numIndividuals = integerQueryResponse.getResult().get(0);

    Map<String, Object> info = new HashMap<>();
    info.put("Number of individuals", numIndividuals.toString());
    beacon.setInfo(info);
    beacon.setSampleAlleleRequests(getSampleAlleleRequests());
    return beacon;
  }


  private List<BeaconAlleleRequest> getSampleAlleleRequests() {
    List<BeaconAlleleRequest> sampleAlleleRequests = new ArrayList<BeaconAlleleRequest>();
    sampleAlleleRequests.add(BeaconAlleleRequest.builder()
            .assemblyId(CsvsConstants.CSVS_ASSEMMBY_ID)
            .referenceName("1")
            .start(69510)
            .includeDatasetResponses(FilterDatasetResponse.NONE)
            .referenceBases("A")
            .alternateBases("G")
            .build());
    sampleAlleleRequests.add(BeaconAlleleRequest.builder()
            .assemblyId(CsvsConstants.CSVS_ASSEMMBY_ID)
            .start(1)
            .end(69512)
            .includeDatasetResponses(FilterDatasetResponse.HIT)
            .datasetIds(new ArrayList<String>(Arrays.asList("1")))
            .filters(new ArrayList<String>(Arrays.asList("myDictionary.tech:1", "ICD-10:VIII")))
            .build());

    return sampleAlleleRequests;
  }

  @Override
  public Object queryBeacon(List<String> datasetStableIds, String variantType,
      String alternateBases, String referenceBases, String chromosome, Integer start,
      Integer startMin, Integer startMax, Integer end, Integer endMin, Integer endMax,
      String referenceGenome, String includeDatasetResponses, List<String> filters) {


    if (StringUtils.isNotBlank(referenceBases)
        && StringUtils.isNotBlank(chromosome) && start != null && StringUtils.isBlank(variantType)
        && startMin == null && startMax == null && endMin == null && endMax == null) {

      if (end == null && StringUtils.isNotBlank(alternateBases)) {

        return genomicQuery
            .queryBeaconGenomicSnp(datasetStableIds, alternateBases, referenceBases, chromosome,
                start, referenceGenome, includeDatasetResponses, filters);
      } else if (end != null && StringUtils.isBlank(alternateBases)) {
        return genomicQuery
            .queryBeaconGenomicRegion(datasetStableIds, referenceBases, chromosome, start, end,
                referenceGenome, includeDatasetResponses, filters);
      } else {
        throw new NotImplementedException("Query not implemented");
      }
    }
    throw new NotImplementedException("Query not implemented");
  }


  @Override
  public List<Integer> checkParams(BeaconAlleleResponse result, List<String> datasetStableIds,
      VariantType type, String alternateBases, String referenceBases, String chromosome,
      Integer start, Integer startMin, Integer startMax, Integer end, Integer endMin,
      Integer endMax, String referenceGenome, List<String> filters,
      List<String> translatedFilters) {
    return null;
  }


  @Override
  public Object queryBeacon(String body) throws IOException {

    BeaconGenomicSnpRequest request = JsonUtils
        .jsonToObject(body, BeaconGenomicSnpRequest.class, objectMapper);

    String includeDatasetResponses =
        request.getIncludeDatasetResponses() != null ? request.getIncludeDatasetResponses()
            .getFilter() : null;

    return queryBeacon(request.getDatasetIds(), null,
        request.getAlternateBases(), request.getReferenceBases(), request.getReferenceName(),
        request.getStart(), null, null, null,
        null, null, request.getAssemblyId(),
        includeDatasetResponses, request.getFilters());
  }
}