/*
 * Copyright (C) 2020 MapRoulette contributors (see CONTRIBUTORS.md).
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package org.maproulette.framework.service

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration

import org.maproulette.framework.model._
import org.maproulette.framework.psql.{Query, _}
import org.maproulette.framework.psql.filter.{BaseParameter, _}
import org.maproulette.framework.repository.TaskReviewRepository
import org.maproulette.framework.mixins.ReviewSearchMixin
import org.maproulette.session.SearchParameters
import org.maproulette.permissions.Permission

import org.maproulette.models.Task
import org.maproulette.models.dal.TaskReviewDAL

/**
  * Service layer for TaskReview
  *
  * @author krotstan
  */
@Singleton
class TaskReviewService @Inject() (
    repository: TaskReviewRepository,
    taskReviewDAL: TaskReviewDAL,
    permission: Permission
) extends ReviewSearchMixin {

  /**
    * Marks expired taskReviews as unnecessary.
    *
    * @param duration - age of task reviews to treat as 'expired'
    * @return The number of taskReviews that were expired
    */
  def expireTaskReviews(duration: FiniteDuration): Int = {
    this.repository.expireTaskReviews(duration)
  }

  /**
    * Gets a list of tasks that have been reviewed (either by this user or requested by this user)
    *
    * @param user      The user executing the request
    * @param reviewTasksType
    * @param searchParameters
    * @return A list of tasks
    */
  def getReviewMetrics(
      user: User,
      reviewTasksType: Int,
      params: SearchParameters,
      onlySaved: Boolean = false,
      excludeOtherReviewers: Boolean = false
  ): ReviewMetrics = {
    val query = this.setupReviewSearchClause(
      Query.empty,
      user,
      permission,
      params,
      reviewTasksType,
      true,
      onlySaved,
      excludeOtherReviewers
    )

    this.repository.executeReviewMetricsQuery(query).head
  }

  /**
    * Gets a list of tasks that have been reviewed (either by this user or requested by this user)
    *
    * @param user      The user executing the request
    * @param searchParameters
    * @param onlySaved Only include saved challenges
    * @return A list of review metrics by mapper
    */
  def getMapperMetrics(
      user: User,
      params: SearchParameters,
      onlySaved: Boolean = false
  ): List[ReviewMetrics] = {
    val query = this.setupReviewSearchClause(
      Query.empty,
      user,
      permission,
      params,
      4,
      true,
      onlySaved,
      excludeOtherReviewers = true
    )

    this.repository.executeReviewMetricsQuery(
      query,
      groupByMappers = true
    )
  }

  /**
    * Gets tasks near the given task id within the given challenge
    *
    * @param challengeId  The challenge id that is the parent of the tasks that you would be searching for
    * @param proximityId  Id of task for which nearby tasks are desired
    * @param excludeSelfLocked Also exclude tasks locked by requesting user
    * @param limit        The maximum number of nearby tasks to return
    * @return
    */
  def getNearbyReviewTasks(
      user: User,
      params: SearchParameters,
      proximityId: Long,
      limit: Int,
      excludeOtherReviewers: Boolean = false,
      onlySaved: Boolean = false
  ): List[Task] = {
    this.taskReviewDAL.getNearbyReviewTasks(
      user,
      params,
      proximityId,
      limit,
      excludeOtherReviewers,
      onlySaved
    )
  }

  /*
   * Gets a list of tag metrics for the review tasks that meet the given
   * criteria.
   *
   * @return A list of tasks
   */
  def getReviewTagMetrics(
      user: User,
      reviewTasksType: Int,
      params: SearchParameters,
      onlySaved: Boolean = false,
      excludeOtherReviewers: Boolean = false
  ): List[ReviewMetrics] = {
    val query = this.setupReviewSearchClause(
      Query.empty,
      user,
      permission,
      params,
      reviewTasksType,
      true,
      onlySaved,
      excludeOtherReviewers
    )

    this.repository.executeReviewMetricsQuery(
      query,
      groupByTags = true
    )
  }
}
