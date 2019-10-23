/**
 * Copyright (C) 2019 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.transform

import java.io.{ StringReader, StringWriter, Writer }

import javax.xml.transform.stream.{ StreamResult, StreamSource }
import javax.xml.transform.{ Source, Transformer }
import nl.knaw.dans.easy.transform.AccessRights.AccessRights
import nl.knaw.dans.easy.transform.bagstore.BagStore

import scala.util.Try
import scala.xml.{ Node, PrettyPrinter, XML }

class EasyTransformMetadataApp(configuration: Configuration) {

  private lazy val prettyPrinter = new PrettyPrinter(160, 2)
  private val bagStore = new BagStore(configuration)
  private val xmlTransformation = new XmlTransformation()

  // TODO implement
  //  (2) enrich files.xml
  //      (a) add <accessibleToRights> and <visibleToRights> if they're not provided; take dataset.xml - ddm:accessRight into account
  //      (b) replace the value in 'filepath' with the download url
  //  (3) combine dataset.xml and files.xml into a single METS xml
  //  (4) if provided, run the transformer to convert the METS xml to the output format and write it to 'output'

  def processDataset(bagId: BagId, transformer: Option[Transformer], output: Writer): Try[Unit] = {
    for {
      datasetXml <- fetchDatasetXml(bagId)
      filesXml <- fetchFilesXml(bagId)
      upgradedFilesXml <- enrichFilesXml(filesXml, AccessRights.withName((datasetXml \ "accessRights").head.text))
      metsXml <- makeMetsXml(datasetXml, upgradedFilesXml)
      resultXml <- transformer.fold(Try { metsXml })(transform(metsXml))
      _ <- outputXml(resultXml, output)
    } yield ()
  }

  def fetchDatasetXml(bagId: BagId): Try[Node] = {
    bagStore.loadDatasetXml(bagId)
  }

  def fetchFilesXml(bagId: BagId): Try[Node] = {
    bagStore.loadFilesXml(bagId)
  }

  private def enrichFilesXml(xml: Node, accessRights: AccessRights): Try[Node] = Try {
    xmlTransformation.enrichFilesXml(xml, accessRights)
  }

  private def makeMetsXml(datasetXml: Node, filesXml: Node): Try[Node] = ???

  private def transform(xml: Node)(transformer: Transformer): Try[Node] = Try {
    val input: Source = new StreamSource(new StringReader(xml.toString()))
    val output = new StringWriter()
    transformer.transform(input, new StreamResult(output))

    XML.loadString(output.toString)
  }

  private def outputXml(xml: Node, output: Writer): Try[Unit] = Try {
    output.write(prettyPrinter.format(xml))
  }
}
