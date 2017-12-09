# MoreChunks Minecraft Mod

Increase render distance on servers with very small default view distance.

[![Discord](https://img.shields.io/discord/268769629575315456.svg?colorB=7289DA&label=Discord)](https://discord.gg/FrZQeFr) [![Build Status](https://travis-ci.org/Gjum/morechunks-forge.svg?branch=master)](https://travis-ci.org/Gjum/morechunks-forge)

![animation visually demonstrating the main principle behind MoreChunks](https://cdn.discordapp.com/attachments/378352993717977090/388103623500103680/demo-4.gif)

## Getting started

[Download the latest `MoreChunks_(version).jar`](https://github.com/Gjum/morechunks-forge/releases/latest)

You can access the settings through Forge's mod list:
- from the main menu: `Mods` -> select `MoreChunks` on the left -> click `Config` at the bottom left
- from the ingame menu: `Mods config` -> select `MoreChunks` on the left -> click `Config` at the bottom left

Or assign a key to `MoreChunks: Open Settings` in `Options` -> `Controls`.

## What is this?

The server you play on has a very short view distance,
and you can't see very far ahead when you're traveling?

In addition to the close-by area that you receive
from the Minecraft game server you're playing on,
this mod loads the remaining area to cover your whole render distance.

The area you receive from the game will be shared with others
when they travel through that area in the future,
and it will look to them the same as to you.

In other words: after someone travels through an area and loads it,
the next time someone comes around, they get sent that area in advance,
before they are close enough for the game server to load it.

Structures below sea level are not shared at all,
so your bunkers are safe by default.

## What information is shared?

Other players have no way of finding out where you are at any time.

When you request some chunks to cover your render distance,
the mod will know that you're within render distance of these chunks.
And looking at all your requests, it's easy to calculate the chunk you're in.
But there's no way for anyone to know your y-coordinate or your exact x and z coordinates within that chunk.

Structures more than 16 blocks below sea level are not shared at all.
This might be configurable more precisely in a future version.

If you need the exact details of the inner workings of the system,
you can look at the server code here: [Gjum/morechunks-server](https://github.com/Gjum/morechunks-server).

## Development

Package the distributable `.jar`: `gradlew reobf`
You can then find it in `build/libs/`.
