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

More on what all the settings mean [below](#settings).

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

## Settings

#### Max. Chunks loaded:
Besides the Render Distance (set in the video settings),
this limits how many chunks are loaded into the game at any time.

You can use this in combination with a very large Render Distance
to avoid distance fog while keeping your memory usage low.

Setting this too small (less than 81 for CivClassic for example)
will result in weird glitches, so keep it fairly large.

#### Loading speed:
How many extra chunks per second are loaded.
If your connection is slow or you experience lag, try setting this to a smaller value.

As long as it's more than double your Render Distance (set in the Video settings),
there won't be any noticeable difference
except when loading the first chunks during login
or when teleporting a far distance (e.g. respawning).

#### Chunk server address:
The [morechunks-server](https://github.com/Gjum/morechunks-server)
instance to use for sharing the chunks.

The default at `gjum.isteinvids.co.uk:12312` is run by Gjum, the creator of this mod.

In the future, you can use MoreChunks on different servers, not just CivClassic,
and at the moment each map needs its own morechunks-server instance.

## Development

Package the distributable `.jar`: `gradlew reobf`
You can then find it in `build/libs/`.
