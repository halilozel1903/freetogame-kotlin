#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running Android TV emulator (1080p).
# D-pad presses can't be timed reliably through adb on a fresh emulator, so the sample opens each scene from the
# `scene` extra. Every scene loads the LIVE FreeToGame API and its images; nothing is bundled and there is no
# fallback. A scene logs SCENE_READY once its data and every image on screen have loaded, and SCENE_FAILED when a
# request fails or FreeToGame answers with a Cloudflare challenge page. Either failure fails the run, so a broken
# screen never replaces a good screenshot.
set -euo pipefail
export SCENE_TAG=FreeToGameScene
source "$(dirname "$0")/screenshot-lib.sh"

install_sample

# The hero for the most popular game, the platform filter and the first rails, "Details" focused.
fresh_launch --es scene home
wait_for_scene home 240
capture tv-home "Popular now"

# Fantasy MMORPGs for Windows, most popular first: GET /filter?tag=mmorpg.fantasy&platform=windows.
fresh_launch --es scene genre
wait_for_scene genre 240
capture tv-genre "games match"

# The most popular Windows game with screenshots and requirements: gallery, facts and minimum requirements.
fresh_launch --es scene detail
wait_for_scene detail 240
capture tv-detail "Minimum system requirements"
