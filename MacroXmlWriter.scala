package org.encalmo.writer.xml

import org.encalmo.utils.AnnotationUtils.*
import org.encalmo.utils.TypeNameUtils.*
import org.encalmo.utils.*

import org.encalmo.writer.xml.XmlOutputBuilder
import scala.quoted.*

object MacroXmlWriter {

  transparent inline def shouldDebugMacroExpansion = false

  inline def write[A](label: String, expr: A)(using
      builder: XmlOutputBuilder
  ): Unit =
    ${ writeImpl[A]('{ label }, '{ expr }, '{ builder }, true) }

  inline def write[A](expr: A)(using
      builder: XmlOutputBuilder
  ): Unit =
    ${ writeImpl[A]('{ expr }, '{ builder }) }

  def writeImpl[A: Type](
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder]
  )(using
      Quotes
  ): Expr[Unit] = {
    writeImpl(typeNameExpr[A], expr, builder, true)
  }

  def writeImpl[A: Type](
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      summonTypeclassInstance: Boolean
  )(using
      Quotes
  ): Expr[Unit] = {
    import quotes.reflect.*

    val term = expr.asTerm match {
      case Inlined(_, _, t) => t
      case t                => t
    }

    val trace = scala.collection.mutable.Buffer.empty[String]
    val annotations = getValueAnnotations(expr)

    val result = writeType[A](
      label,
      term.asExprOf[A],
      builder,
      hasTag = false,
      isCollectionItem = false,
      currentAnnotations = annotations,
      trace = trace,
      debugIndent = 0,
      summonTypeclassInstance = summonTypeclassInstance
    )
    if shouldDebugMacroExpansion then report.warning(trace.mkString("\n"))
    result
  }

  /** Entry method to write the value of an unknown type to the XML output. */
  def writeType[A: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      currentAnnotations: Set[AnnotationInfo],
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      summonTypeclassInstance: Boolean = true
  ): Expr[Unit] = {
    import quotes.reflect.*

    val tagName: Expr[String] = currentAnnotations
      .getOrDefault[annotation.xmlTag, String](
        parameter = "name",
        defaultValue = label
      )

    val isXmlContent: Boolean = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag: Boolean = !hasTag && !isXmlContent

    val maybeTagValue: Option[Expr[String]] = currentAnnotations
      .get[annotation.xmlValue, String](parameter = "value")

    val maybeTagValueSelector: Option[Expr[String]] = currentAnnotations
      .get[annotation.xmlValueSelector, String](parameter = "property")

    if shouldDebugMacroExpansion then
      tagName.value
        .orElse(Some("unknown"))
        .foreach(l =>
          trace.append(
            "-" * debugIndent + " " + currentAnnotations
              .map(_.toString)
              .mkString("", " ", " ") + l + ": " + TypeNameUtils
              .shortBaseName(
                TypeRepr.of[A].show
              ) + " hasTag=" + hasTag + " flags:" + TypeRepr.of[A].typeSymbol.flags.show
          )
        )

    def generateWriterExpressions = {
      if TypeRepr.of[A].dealias.typeSymbol.isTypeParam then
        report.errorAndAbort(
          s"""${TypeRepr
              .of[A]
              .show} is an abstract type parameter and cannot be serialized to XML. 
  Possible solutions:
  - Add inline keyword to the method definition.
  - Add (using XmlWriter[${TypeRepr.of[A].show}]) to the method definition
  - Define a given XmlWriter[${TypeRepr.of[A].show}] in the current scope
  """
        )
      else
        TypeRepr.of[A].dealias.asType match {

          case '[String] | '[Boolean] | '[Int] | '[Long] | '[Float] | '[Double] | '[BigDecimal] | '[BigInt] | '[Char] |
              '[Short] | '[Byte] =>
            debug(trace, debugIndent, tagName.value.getOrElse("unknown"), "writePrimitive", hasTag)
            '{
              if ${ Expr(shouldTag) } then ${ builder }.appendElementStart(${ tagName })
              ${ builder }.appendText(${ expr }.toString)
              if ${ Expr(shouldTag) } then ${ builder }.appendElementEnd(${ tagName })
            }

          case '[t] if TupleUtils.isTuple[t] || TupleUtils.isNamedTuple[t] =>
            writeTuple[A](
              label = tagName,
              itemLabel = None,
              expr = expr,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = currentAnnotations,
              debugIndent = debugIndent
            )

          // Handle Option[T] special way, omit the element if the value is None
          case '[Option[t]] =>
            writeOption[t](
              label = tagName,
              expr = '{ ${ expr }.asInstanceOf[Option[t]] },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = currentAnnotations,
              debugIndent = debugIndent
            )

          case '[Either[left, right]] =>
            writeEither[left, right](
              label = tagName,
              expr = '{ ${ expr }.asInstanceOf[Either[left, right]] },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = currentAnnotations,
              debugIndent = debugIndent
            )

          case '[scala.collection.Map[k, v]] =>
            writeMap[k, v](
              label = tagName,
              expr = '{ ${ expr }.asInstanceOf[scala.collection.Map[k, v]] },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = currentAnnotations,
              debugIndent = debugIndent
            )

          case '[scala.collection.Iterable[t]] =>
            writeCollection[t](
              label = tagName,
              expr = '{ ${ expr }.asInstanceOf[scala.collection.Iterable[t]].iterator },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = currentAnnotations,
              debugIndent = debugIndent
            )

          case '[Array[t]] =>
            writeCollection[t](
              label = tagName,
              expr = '{ ${ expr }.asInstanceOf[Array[t]].iterator },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = currentAnnotations,
              debugIndent = debugIndent
            )

          case '[t] if CaseClassUtils.isCaseClass[t] =>
            // prepare list of attributes for the xml opening tag
            val attributes: Expr[Iterable[(String, String)]] =
              collectAttributesFromCaseClass(tagName, expr, builder, trace, currentAnnotations)

            '{
              if ${ Expr(shouldTag) } then ${ builder }.appendElementStart(${ tagName }, ${ attributes })
              ${
                writeCaseClass[A](
                  tagName,
                  expr,
                  builder,
                  !hasTag,
                  isCollectionItem,
                  trace,
                  currentAnnotations,
                  debugIndent
                )
              }
              if ${ Expr(shouldTag) } then ${ builder }.appendElementEnd(${ tagName })
            }

          case '[t] if EnumUtils.isEnumOrSealedADT[t] || EnumUtils.isJavaEnum[t] =>
            writeEnum[A](
              tagName,
              expr,
              builder,
              hasTag,
              isCollectionItem,
              trace,
              currentAnnotations,
              debugIndent
            )

          case '[t] if UnionUtils.isUnion[t] =>
            '{
              if ${ Expr(shouldTag) } then ${ builder }.appendElementStart(${ tagName })
              ${
                writeUnion[A](
                  tagName,
                  expr,
                  builder,
                  !hasTag,
                  isCollectionItem,
                  trace,
                  currentAnnotations,
                  debugIndent
                )
              }
              if ${ Expr(shouldTag) } then ${ builder }.appendElementEnd(${ tagName })
            }

          case '[t] if JavaRecordUtils.isJavaRecord[t] =>
            '{
              if ${ Expr(shouldTag) } then ${ builder }.appendElementStart(${ tagName })
              ${
                writeJavaRecord[A](
                  label = tagName,
                  expr = expr,
                  builder = builder,
                  hasTag = hasTag,
                  isCollectionItem = isCollectionItem,
                  trace = trace,
                  currentAnnotations = currentAnnotations,
                  debugIndent = debugIndent
                )
              }
              if ${ Expr(shouldTag) } then ${ builder }.appendElementEnd(${ tagName })
            }

          case _ =>
            SelectableUtils
              .maybeVisitSelectable[A](functionExpr = { [Fields: Type] => Quotes ?=>
                '{
                  if ${ Expr(shouldTag) } then ${ builder }.appendElementStart(${ tagName })
                  ${
                    SelectableUtils.visitFields[A, Fields](
                      expr,
                      functionExpr = { [A: Type] => Quotes ?=> (name, value) =>
                        writeType[A](
                          label = name,
                          expr = value,
                          builder = builder,
                          hasTag = hasTag,
                          isCollectionItem = isCollectionItem,
                          trace = trace,
                          currentAnnotations = currentAnnotations,
                          debugIndent = debugIndent
                        )
                      }
                    )
                  }
                  if ${ Expr(shouldTag) } then ${ builder }.appendElementEnd(${ tagName })
                }
              })
              .getOrElse(
                JavaMapUtils
                  .maybeVisitJavaMap[A](functionExpr = { [K: Type, V: Type] => Quotes ?=>
                    '{
                      if ${ Expr(shouldTag) } then ${ builder }.appendElementStart(${ tagName })
                      ${
                        writeJavaMap[K, V](
                          label = tagName,
                          expr = '{ ${ expr }.asInstanceOf[java.util.Map[K, V]] },
                          builder = builder,
                          hasTag = hasTag,
                          isCollectionItem = isCollectionItem,
                          trace = trace,
                          currentAnnotations = currentAnnotations,
                          debugIndent = debugIndent
                        )
                      }
                      if ${ Expr(shouldTag) } then ${ builder }.appendElementEnd(${ tagName })
                    }
                  })
                  .getOrElse(
                    JavaIterableUtils
                      .maybeVisitJavaIterable[A](
                        functionExpr = { [B: Type] => Quotes ?=>
                          writeJavaIterable[B](
                            label = tagName,
                            expr = '{ ${ expr }.asInstanceOf[java.lang.Iterable[B]] },
                            builder = builder,
                            hasTag = hasTag,
                            isCollectionItem = isCollectionItem,
                            trace = trace,
                            currentAnnotations = currentAnnotations,
                            debugIndent = debugIndent
                          )
                        }
                      )
                      .getOrElse(
                        maybeWriteOpaqueType[A](
                          tagName,
                          expr,
                          builder,
                          hasTag,
                          isCollectionItem,
                          trace,
                          currentAnnotations,
                          debugIndent
                        )
                      )
                  )
              )
        }
    }

    def tryStaticValueOrSelectorExpression = {
      maybeTagValue match {
        case Some(value) =>
          debug(trace, debugIndent, tagName.value.getOrElse("unknown"), "writeXmlValue", hasTag)
          '{
            if ${ Expr(shouldTag) } then ${ builder }.appendElementStart(${ tagName })
            ${ builder }.appendText(${ value })
            if ${ Expr(shouldTag) } then ${ builder }.appendElementEnd(${ tagName })
          }

        case None =>
          maybeTagValueSelector.flatMap(_.value).match {
            case Some(selector) => {
              MethodUtils.maybeSelectedValue[A](
                selector = selector,
                label = label,
                expr = expr,
                functionExpr = { [A: Type] => Quotes ?=> (name, value) =>
                  debug(
                    trace,
                    debugIndent,
                    name.value.getOrElse("unknown"),
                    "writeType " + TypeRepr.of[A].show(using Printer.TypeReprShortCode)
                      + " selected by " + selector,
                    hasTag
                  )
                  writeType[A](
                    label = label,
                    expr = value,
                    builder = builder,
                    hasTag = hasTag,
                    isCollectionItem = isCollectionItem,
                    currentAnnotations = currentAnnotations,
                    trace = trace,
                    debugIndent = debugIndent + 1
                  )
                },
                fallbackExpr = {
                  generateWriterExpressions
                }
              )
            }
            case None => generateWriterExpressions
          }
      }
    }

    if (summonTypeclassInstance) then
      Expr.summon[XmlWriter[A]] match {
        case Some(writer) =>
          debug(
            trace,
            debugIndent,
            tagName.value.getOrElse("unknown"),
            s"use XmlWriter[${TypeRepr.of[A].show}] createTag=${shouldTag}",
            hasTag
          )
          '{
            ${ writer }.write(${ tagName }, ${ expr }, ${ Expr(shouldTag) })(using ${ builder })
          }

        case None => tryStaticValueOrSelectorExpression
      }
    else tryStaticValueOrSelectorExpression

  }

  def collectAttributesFromCaseClass[A: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo]
  ): Expr[Iterable[(String, String)]] =
    Expr.ofList(
      CaseClassUtils.transform(
        expr,
        { [B: Type] => Quotes ?=> (name, value, annotations) =>
          {
            // if the field is annotated with @xmlTag,
            // then use the name from the annotation
            val label = annotations
              .getOrDefault[annotation.xmlTag, String](
                parameter = "name",
                defaultValue = name
              )

            if annotations.exists[annotation.xmlAttribute]
            then
              Some('{
                (${ label }, ${ value }.toString)
              })
            else None
          }
        }
      )
    )

  def writeCaseClass[A: Type](
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  )(using Quotes): Expr[Unit] = {
    import quotes.reflect.*
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeCaseClass", hasTag)
    MethodUtils.wrapInMethodCall(
      "writeCaseClassToXml_" + TypeNameUtils.underscored(TypeRepr.of[A].show),
      // visit the case class and write each field
      CaseClassUtils.visit[A](
        expr,
        {
          [B: Type] => Quotes ?=> (
              name,
              value,
              annotations
          ) =>
            {
              val isAttribute =
                annotations.exists[annotation.xmlAttribute]

              // skip field if was aleady written as an attribute
              if !isAttribute then {
                writeType[B](
                  label = name,
                  expr = value,
                  builder = builder,
                  hasTag = false,
                  isCollectionItem = false,
                  currentAnnotations = AnnotationUtils.annotationsOf[B] ++ annotations.computeInfo,
                  trace = trace,
                  debugIndent = debugIndent + 1
                )
              } else '{}
            }
        }
      )
    )
  }

  def writeEnum[A: Type](
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  )(using Quotes): Expr[Unit] = {
    import quotes.reflect.*
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeEnum", hasTag)
    val typeAnnotations = AnnotationUtils.annotationsOf[A]
    MethodUtils.wrapInMethodCall(
      "writeEnumToXml_" + TypeNameUtils.underscored(TypeRepr.of[A].show),
      // visit the case class and write each field
      EnumUtils.visit[A](
        valueExpr = expr,
        functionWhenCaseValueExpr = { [B: Type] => Quotes ?=> (name, value, annotations) =>
          val allAnnotations = typeAnnotations ++ currentAnnotations ++ annotations.computeInfo

          debug(
            trace,
            debugIndent,
            label.value.getOrElse("unknown") + "." + name.value.getOrElse("unknown"),
            allAnnotations.map(_.toString).mkString("", " ", " ") + "functionWhenCaseValueExpr",
            hasTag
          )

          val tagValue: Expr[String] = allAnnotations
            .getOrDefault[annotation.xmlValue, String](
              parameter = "value",
              defaultValue = '{ ${ expr }.toString }
            )

          if hasTag then {
            if isCollectionItem then {
              '{
                ${ builder }.appendElementStart(${ name })
                ${ builder }.appendElementEnd(${ name })
              }
            } else {
              '{ ${ builder }.appendText(${ tagValue }) }
            }
          } else {
            '{
              ${ builder }.appendElementStart(${ label })
              ${ builder }.appendText(${ tagValue })
              ${ builder }.appendElementEnd(${ label })
            }
          }
        },
        functionWhenCaseClassExpr = { [B: Type] => Quotes ?=> (name, value, annotations) =>
          val allAnnotations = typeAnnotations ++ currentAnnotations ++ annotations.computeInfo

          debug(
            trace,
            debugIndent,
            label.value.getOrElse("unknown") + "." + name.value.getOrElse("unknown"),
            allAnnotations
              .map(_.toString)
              .mkString("", " ", " ") + "functionWhenCaseClassExpr isCollectionItem=" + isCollectionItem,
            hasTag
          )

          val body = writeType[B](
            label = name,
            expr = value,
            builder = builder,
            hasTag = hasTag,
            isCollectionItem = isCollectionItem,
            currentAnnotations = allAnnotations,
            trace = trace,
            debugIndent = debugIndent + 1
          )

          val useEnumCaseNames = allAnnotations.exists[annotation.xmlUseEnumCaseNames]
          val skipTagInsideCollection = allAnnotations.exists[annotation.xmlNoTagInsideCollection]

          if hasTag then {
            if isCollectionItem then {
              '{
                ${ builder }.appendElementStart(${ name })
                ${ body }
                ${ builder }.appendElementEnd(${ name })
              }
            } else {
              body
            }
          } else {
            if isCollectionItem && !skipTagInsideCollection then {
              '{
                ${ builder }.appendElementStart(${ label })
                ${ body }
                ${ builder }.appendElementEnd(${ label })
              }
            } else {
              writeType[B](
                label = if useEnumCaseNames then name else label,
                expr = value,
                builder = builder,
                hasTag = hasTag,
                isCollectionItem = isCollectionItem,
                currentAnnotations = allAnnotations,
                trace = trace,
                debugIndent = debugIndent + 1
              )

            }
          }
        }
      )
    )
  }

  def writeUnion[A: Type](using
      q: Quotes
  )(
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    import quotes.reflect.*
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeUnion", hasTag)
    MethodUtils.wrapInMethodCall(
      "writeUnionToXml_" + TypeNameUtils.underscored(TypeRepr.of[A].show),
      UnionUtils.visit[A](
        label,
        expr,
        { [B: Type] => Quotes ?=> (name, value) =>
          writeType[B](
            label = name,
            expr = value,
            builder = builder,
            hasTag = hasTag,
            isCollectionItem = isCollectionItem,
            currentAnnotations = AnnotationUtils.annotationsOf[B] ++ currentAnnotations,
            trace = trace,
            debugIndent = debugIndent + 1
          )
        }
      )
    )
  }

  def writeOption[A: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[Option[A]],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] =
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeOption", hasTag)
    '{
      ${ expr } match {
        case Some(value) =>
          ${
            writeType[A](
              label = '{
                if (${ Expr(isCollectionItem) } && ${ label } == "Option") then ${ typeNameExpr[A] } else ${ label }
              },
              expr = '{ value },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              currentAnnotations = AnnotationUtils.annotationsOf[A] ++ currentAnnotations,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        case None => {}
      }
    }

  def writeEither[Left: Type, Right: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[Either[Left, Right]],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeEither", hasTag)
    '{
      ${ expr } match {
        case Left(value) =>
          ${
            writeType[Left](
              label = label,
              expr = '{ ${ expr }.swap.toOption.get },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              currentAnnotations = currentAnnotations,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        case Right(value) =>
          ${
            writeType[Right](
              label = label,
              expr = '{ ${ expr }.toOption.get },
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              currentAnnotations = currentAnnotations,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
      }
    }
  }

  def writeCollection[A: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[scala.collection.Iterator[A]],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeCollection", hasTag)
    // get the name of the item tag from the annotation or default to the type name
    val itemLabel =
      currentAnnotations.getOrDefault[annotation.xmlItemTag, String](
        parameter = "name",
        defaultValue = typeNameExpr[A]
      )
    // check if the item tags should be skipped
    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    '{
      if (${ Expr(shouldTag) }) then ${ builder }.appendElementStart(${ label })
      ${ expr }.foreach { value =>
        ${
          writeType[A](
            label = itemLabel,
            expr = '{ value },
            builder = builder,
            hasTag = skipItemTags,
            isCollectionItem = true,
            currentAnnotations = AnnotationUtils.annotationsOf[A] ++ getValueAnnotations('{ value }),
            trace = trace,
            debugIndent = debugIndent + 1
          )
        }
      }
      if (${ Expr(shouldTag) }) then ${ builder }.appendElementEnd(${ label })
    }
  }

  def writeMap[K: Type, V: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[scala.collection.Map[K, V]],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeMap", hasTag)
    '{
      ${ expr }.foreach { (key, value) =>
        ${
          writeType[V](
            label = '{ key.toString() },
            expr = '{ value },
            builder = builder,
            hasTag = false,
            isCollectionItem = true,
            currentAnnotations = AnnotationUtils.annotationsOf[V] ++ getValueAnnotations('{ value }),
            trace = trace,
            debugIndent = debugIndent + 1
          )
        }
      }
    }
  }

  def writeJavaIterable[A: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[java.lang.Iterable[A]],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    import quotes.reflect.*
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeJavaIterable", hasTag)

    val itemLabel =
      currentAnnotations.getOrDefault[annotation.xmlItemTag, String](
        parameter = "name",
        defaultValue = typeNameExpr[A]
      )
    // check if the item tags should be skipped
    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    TypeRepr.of[A].dealias.asType match {
      case '[t] =>
        '{
          if (${ Expr(shouldTag) }) then ${ builder }.appendElementStart(${ label })
          val iterator = ${ expr }.iterator()
          while (iterator.hasNext) {
            val value = iterator.next().asInstanceOf[t]
            ${
              writeType[t](
                label = itemLabel,
                expr = '{ value },
                builder = builder,
                hasTag = hasTag,
                isCollectionItem = true,
                currentAnnotations = AnnotationUtils.annotationsOf[A] ++ getValueAnnotations('{ value }),
                trace = trace,
                debugIndent = debugIndent + 1
              )
            }
          }
          if (${ Expr(shouldTag) }) then ${ builder }.appendElementEnd(${ label })
        }
    }
  }

  def writeJavaMap[K: Type, V: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[java.util.Map[K, V]],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeJavaMap", hasTag)
    '{
      val it = ${ expr }.entrySet().iterator()
      while (it.hasNext) {
        val entry = it.next()
        val key: K = entry.getKey
        val value: V = entry.getValue
        ${
          writeType[V](
            label = '{ key.toString() },
            expr = '{ value },
            builder = builder,
            hasTag = hasTag,
            isCollectionItem = isCollectionItem,
            currentAnnotations = AnnotationUtils.annotationsOf[V] ++ getValueAnnotations('{ value }),
            trace = trace,
            debugIndent = debugIndent + 1
          )
        }
      }
    }
  }

  def maybeWriteOpaqueType[A: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    OpaqueTypeUtils.visit[A](
      label = label,
      valueExpr = expr,
      functionWhenOpaqueTypeExpr = { [B: Type] => Quotes ?=> (name, value) =>
        debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeOpaqueType", hasTag)
        writeType[B](
          label = name,
          expr = value,
          builder = builder,
          hasTag = hasTag,
          isCollectionItem = isCollectionItem,
          currentAnnotations = AnnotationUtils.annotationsOf[B] ++ currentAnnotations,
          trace = trace,
          debugIndent = debugIndent + 1
        )
      },
      functionWhenOtherExpr = { [B: Type] => Quotes ?=> (name, value) =>
        debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeAsString", hasTag)
        // default to writing the string representation of the value
        '{
          if ${ Expr(!hasTag) } then ${ builder }.appendElementStart(${ label })
          ${ builder }.appendText(${ expr }.toString)
          if ${ Expr(!hasTag) } then ${ builder }.appendElementEnd(${ label })
        }
      }
    )
  }

  def writeTuple[A: Type](using
      Quotes
  )(
      label: Expr[String],
      itemLabel: Option[Expr[String]],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeTuple", hasTag)

    // check if the item tags should be skipped
    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    val itemTag: Option[Expr[String]] = itemLabel.orElse(
      currentAnnotations.get[annotation.xmlItemTag, String](parameter = "name")
    )

    TupleUtils.visit[A](
      label = itemTag,
      valueExpr = expr,
      onStart = '{
        if ${ Expr(shouldTag) }
        then ${ builder }.appendElementStart(${ label })
      },
      functionWhenTupleExpr = { [B: Type] => Quotes ?=> (name, value, index) =>
        debug(trace, debugIndent, name.flatMap(_.value).getOrElse("unknown"), "functionWhenTupleExpr", skipItemTags)
        writeType[B](
          label = name.getOrElse(typeNameExpr[B]),
          expr = '{ $expr.asInstanceOf[Product].productElement(${ index }).asInstanceOf[B] },
          builder = builder,
          hasTag = skipItemTags,
          isCollectionItem = true,
          currentAnnotations = AnnotationUtils.annotationsOf[B] ++ currentAnnotations,
          trace = trace,
          debugIndent = debugIndent + 1
        )
      },
      functionWhenNamedTupleExpr = { [B: Type] => Quotes ?=> (name, value, index) =>
        debug(trace, debugIndent, name.flatMap(_.value).getOrElse("unknown"), "functionWhenNamedTupleExpr", false)
        writeType[B](
          label = name.getOrElse(typeNameExpr[B]),
          expr = '{ $expr.asInstanceOf[Product].productElement(${ index }).asInstanceOf[B] },
          builder = builder,
          hasTag = false,
          isCollectionItem = false,
          currentAnnotations = AnnotationUtils.annotationsOf[B] ++ currentAnnotations,
          trace = trace,
          debugIndent = debugIndent + 1
        )
      },
      onEnd = '{
        if ${ Expr(shouldTag) }
        then ${ builder }.appendElementEnd(${ label })
      }
    )
  }

  def writeJavaRecord[A: Type](using
      Quotes
  )(
      label: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Expr[Unit] = {
    debug(trace, debugIndent, label.value.getOrElse("unknown"), "writeJavaRecord", hasTag)
    JavaRecordUtils.visit[A](
      label = label,
      valueExpr = expr,
      functionExpr = { [B: Type] => Quotes ?=> (name, value) =>
        writeType[B](
          label = name,
          expr = value,
          builder = builder,
          hasTag = hasTag,
          isCollectionItem = isCollectionItem,
          currentAnnotations = AnnotationUtils.annotationsOf[A] ++ currentAnnotations,
          trace = trace,
          debugIndent = debugIndent + 1
        )
      }
    )
  }

  inline def debug(
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      label: String,
      message: String,
      hasTag: Boolean
  ): Unit = {
    inline if shouldDebugMacroExpansion then
      trace.append(">" * debugIndent + "> " + label + ": " + message + " hasTag=" + hasTag)
  }
}
