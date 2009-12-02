/* ------------------------------------------------------------------
 * Copyright (C) 1998-2009 PacketVideo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 * -------------------------------------------------------------------
 */
#ifndef PV_2WAY_REC_DATAPATH_H_INCLUDED
#define PV_2WAY_REC_DATAPATH_H_INCLUDED


#ifndef PV_2WAY_DATAPATH_H_INCLUDED
#include "pv_2way_datapath.h"
#endif

#ifndef PV_2WAY_CMD_CONTROL_DATAPATH_H_INCLUDED
#include "pv_2way_cmd_control_datapath.h"
#endif

//Forward declaration
class CPV2WayDataChannelDatapath;

class CPV2WayRecDatapath : public CPV2WayCmdControlDatapath
{
    public:
        static CPV2WayRecDatapath *NewL(PVLogger *aLogger,
                                        TPV2WayMediaType aFormat,
                                        CPV2WayDataChannelDatapath &aDatapath,
                                        CPV324m2Way *a2Way);

        virtual ~CPV2WayRecDatapath() {};

        bool SetCmd(TPV2WayCmdInfo *aCmdInfo);

    private:
        CPV2WayRecDatapath(PVLogger *aLogger,
                           PVMFFormatType aFormat,
                           CPV2WayDataChannelDatapath &aDatapath,
                           CPV324m2Way *a2Way) : CPV2WayCmdControlDatapath(aLogger, ERecDatapath, aFormat, a2Way),
                iChannelDatapath(&aDatapath)
        {};

        //Override base virtuals
        void CheckPause();
        void CheckResume();

        void OpenComplete();
        void PauseComplete()
        {
            CommandComplete(PVMFSuccess);
        }
        void ResumeComplete()
        {
            CommandComplete(PVMFSuccess);
        }
        void CloseComplete();

        void DatapathError();

        bool ParentIsClosing();

        CPV2WayDataChannelDatapath *iChannelDatapath;
};


#endif //PV_2WAY_REC_DATAPATH_H_INCLUDED


