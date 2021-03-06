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

package uk.ac.bbsrc.tgac.miso.core.service.tagbarcode;

import net.sourceforge.fluxion.spi.Spi;
import uk.ac.bbsrc.tgac.miso.core.data.TagBarcode;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;

import java.util.Map;
import java.util.Set;

/**
 * uk.ac.bbsrc.tgac.miso.core.service.tagbarcode
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @date 28/06/12
 * @since 0.1.6
 */
@Spi
public interface TagBarcodeStrategy {
  String getName();
  PlatformType getPlatformType();
  int getNumApplicableBarcodes();
  Map<Integer, Set<TagBarcode>> getApplicableBarcodes();
  Set<TagBarcode> getApplicableBarcodesForPosition(int position);
  void reload();
}
