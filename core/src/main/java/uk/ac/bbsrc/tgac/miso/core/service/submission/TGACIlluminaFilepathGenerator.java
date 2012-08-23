/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.core.service.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.bbsrc.tgac.miso.core.data.*;
import uk.ac.bbsrc.tgac.miso.core.data.impl.*;
import uk.ac.bbsrc.tgac.miso.core.exception.SubmissionException;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: collesa
 * Date: 04/04/12
 * Time: 15:15
 * To change this template use File | Settings | File Templates.
 */
public class TGACIlluminaFilepathGenerator implements FilePathGenerator {
  @Autowired
  private RequestManager requestManager;

  protected static final Logger log = LoggerFactory.getLogger(TGACIlluminaFilepathGenerator.class);

  @Override
  public File generateFilePath(SequencerPoolPartition partition, Dilution l) throws SubmissionException {
    Pool pool = partition.getPool();
    if (pool != null) {
      if (pool.getExperiments() != null) {

        Collection<Experiment> experiments = pool.getExperiments();
        Experiment experiment = experiments.iterator().next();
        //String filePath = lane.getFlowcell().getRun().getFilePath()+"/Data/Intensities/BaseCalls/PAP/Project_"+
          log.debug("Partition Container: " + partition.getSequencerPartitionContainer());
          log.debug("Run: " + partition.getSequencerPartitionContainer().getRun());
          log.debug("Filepath: " + partition.getSequencerPartitionContainer().getRun().getFilePath());
          log.debug("Project alias: " + experiment.getStudy().getProject().getAlias());
          log.debug("Library: " + l.getLibrary().getName());
         // log.debug("Library Tag Barcode: " + l.getLibrary().getTagBarcode());
         // log.debug("Library Tag Barcode Sequence: " + l.getLibrary().getTagBarcode().getSequence());
          StringBuilder filePath = new StringBuilder();
        filePath.append(partition.getSequencerPartitionContainer().getRun().getFilePath() + "/Data/Intensities/BaseCalls/PAP/Project_" +
                          experiment.getStudy().getProject().getAlias() + "/Sample_" + l.getLibrary().getName() + "/" +
                          l.getLibrary().getName());
        if(l.getLibrary().getTagBarcodes()!=null && !l.getLibrary().getTagBarcodes().isEmpty()){
          filePath.append("_");
          for (TagBarcode tb : l.getLibrary().getTagBarcodes().values()) {
            filePath.append(tb.getSequence());
          }
        }
        filePath.append("_L00" + partition.getPartitionNumber() + "*.fastq.gz");
        log.debug("Filepath: " + filePath);
        File file = new File(filePath.toString());
        return (file);
      }
      else {
        throw new SubmissionException("partition.getPool=null!");
      }
    }
    else {
      throw new SubmissionException("Collection of experiments is empty");
    }
  }


  public String generateFileName(LibraryDilution l, SequencerPoolPartition p){

      String fileName= l.getLibrary().getName()+"/"
                      +l.getLibrary().getName()+"_"+l.getLibrary().getTagBarcode().getSequence()+
                      "L00"+p.getPartitionNumber();
      return fileName;
  }

  @Override
  public Set<File> generateFilePaths(SequencerPoolPartition partition) throws SubmissionException {
    log.debug("Generating filepaths for partition " + partition.getId());
    Set<File> filePaths = new HashSet<File>();
    log.debug(partition.getSequencerPartitionContainer().getName());


    log.debug(partition.getSequencerPartitionContainer().getRun().getName());
    log.debug(partition.getSequencerPartitionContainer().getRun().getFilePath());
    if((partition.getSequencerPartitionContainer().getRun().getFilePath())==null){
          log.debug("no runfilepath");
          throw new SubmissionException("no runfilepath!");
      }

    Pool pool = partition.getPool();
    if (pool == null) {
      throw new SubmissionException("partition.getPool=null!");
    }
    else {
      Collection<Experiment> experiments = pool.getExperiments();
      if (experiments.isEmpty()) {
        throw new SubmissionException("Collection or experiments is empty");
      }
      else {
        Collection<LibraryDilution> libraryDilutions = pool.getDilutions();
        if (libraryDilutions.isEmpty()) {
          throw new SubmissionException("Collection of libraryDilutions is empty");
        }
        else {
          for (LibraryDilution l : libraryDilutions) {
            File file=generateFilePath(partition,l);
            filePaths.add(file);
          }
        }
      }
    }
    return (filePaths);
  }

  private class IlluminaFilenameFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String name) {
      Pattern pattern = Pattern.compile("LIB[\\d]+_[ACTG]+_L00[\\d]{1}_.*\\.fastq.gz");
      Matcher m = pattern.matcher(name);
      return m.matches();
    }
  }
}